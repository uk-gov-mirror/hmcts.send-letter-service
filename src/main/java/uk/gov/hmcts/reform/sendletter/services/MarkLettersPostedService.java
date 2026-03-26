package uk.gov.hmcts.reform.sendletter.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.entity.Report;
import uk.gov.hmcts.reform.sendletter.entity.ReportRepository;
import uk.gov.hmcts.reform.sendletter.exception.FtpDownloadException;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintStatus;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;
import uk.gov.hmcts.reform.sendletter.model.out.PostedReportTaskResponse;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.IFtpAvailabilityChecker;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

/**
 * Fetches reports from SFTP concerning posted
 * letters and sets status as Posted in the database.
 */
@Service
@RequiredArgsConstructor
public class MarkLettersPostedService {

    /**
     * Used to store the calculated report details, allowing limited calls to the DB.
     *
     * @param reportCode      the report code
     * @param reportDate      the report date
     * @param isInternational the international status
     */
    private record ReportInfo(String reportCode, LocalDate reportDate, boolean isInternational) {
    }

    private final LetterDataAccessService dataAccessService;
    private final LetterService letterService;
    private final FtpClient ftpClient;
    private final IFtpAvailabilityChecker ftpAvailabilityChecker;
    private final ReportParser parser;
    private final AppInsights insights;
    private final ReportsServiceConfig reportsServiceConfig;

    private static final Logger logger = LoggerFactory.getLogger(MarkLettersPostedService.class);
    private static final String TASK_NAME = "MarkLettersPosted";
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}|\\d{2})-\\d{2}-(\\d{4}|\\d{2})");
    private static final Pattern REPORT_CODE_PATTERN = Pattern.compile("(?<=MOJ_)[^_.]+");

    private final ReportRepository reportRepository;

    // default formatter used when resolving dates from a report path
    public static final DateTimeFormatter REPORT_DATE_FORMATTER;

    static {
        REPORT_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .toFormatter();
    }

    /**
     * Fetches reports from SFTP and sets status as Posted in the database.
     */
    public List<PostedReportTaskResponse> processReports() {
        if (!ftpAvailabilityChecker.isFtpAvailable(LocalTime.now(ZoneId.of(EUROPE_LONDON)))) {
            logger.info("Not processing '{}' task due to FTP downtime window", TASK_NAME);
            return Collections.emptyList();
        }
        logger.info("Started '{}' task", TASK_NAME);
        final AtomicReference<PostedReportTaskResponse> currentResponse = new AtomicReference<>();
        final List<PostedReportTaskResponse> responseList = new ArrayList<>();
        try {
            ftpClient
                .downloadReports()
                .stream()
                .map(parser::parse)
                .forEach(parsedReport -> {

                    insights.trackPrintReportReceived(parsedReport);
                    logger.info("Updating letters from report {}. Letter count: {}",
                        parsedReport.path, parsedReport.statuses.size());

                    ReportInfo reportInfo = extractReportInfoFromParsedReport(parsedReport);

                    if (reportInfo == null) {
                        // this is an edge case where the report filename didn't contain a know reportCode
                        // and there were no letters referenced in the parsed report that could be used
                        // to determine a report code from their assigned service.
                        //
                        // When this happens, processing is allowed to move on to the next parsed report,
                        // but an error response will be added to indicate that a report couldn't be married
                        // up to a specific service.
                        currentResponse.set(new PostedReportTaskResponse(
                            "UNKNOWN",
                            parsedReport.reportDate,
                            false)
                        );
                        currentResponse.get().markAsFailed(
                            String.format("Service not found for report with name '%s'", parsedReport.path));
                    } else {

                        // initialise a response now so that we can ensure a response in the case of an
                        // exceptional failure during the markAsPosted iteration below.
                        currentResponse.set(new PostedReportTaskResponse(
                            reportInfo.reportCode,
                            reportInfo.reportDate,
                            reportInfo.isInternational)
                        );

                        long count = parsedReport.statuses.stream()
                            .filter(status -> markAsPosted(status, parsedReport.path))
                            .count();

                        currentResponse.get().setMarkedPostedCount(count);

                        if (parsedReport.allRowsParsed) {
                            logger.info("Report {} successfully parsed, deleting", parsedReport.path);
                            ftpClient.deleteReport(parsedReport.path);
                            // now that we've processed the file, we can save a report.
                            reportRepository.save(Report.builder()
                                .reportName(parsedReport.path)
                                .reportCode(reportInfo.reportCode)
                                .reportDate(reportInfo.reportDate)
                                .printedLettersCount(count)
                                .isInternational(reportInfo.isInternational)
                                .build()
                            );
                        } else {
                            logger.warn("Report {} contained invalid rows, file not removed.", parsedReport.path);
                            currentResponse.get().markAsFailed("Report "
                                + parsedReport.path + " contained invalid rows");
                        }
                    }
                    responseList.add(currentResponse.getAndSet(null));
                });

            logger.info("Completed '{}' task", TASK_NAME);
        } catch (Exception e) {
            logger.error("An error occurred when downloading reports from SFTP server", e);
            // If we opened a response before the exception was thrown, we need to commit
            // that response to the list and mark it up as an error.
            Optional.ofNullable(currentResponse.getAndSet(null)).ifPresentOrElse(ptr -> {
                ptr.markAsFailed(
                    "An error occurred when processing downloaded reports from the SFTP server: " + e.getMessage());
                responseList.add(ptr);
            }, () -> {
                // otherwise, make a choice about adding a general error or just throwing a 503
                if (responseList.isEmpty()) {
                    // if we have an empty response list at this point, we should
                    // throw a formal 503
                    throw new FtpDownloadException("An error occurred when downloading reports from SFTP server", e);
                } else {
                    // if the response list already has some errors in it then we need to add
                    // another one with an UNKNOWN report code that details the error
                    PostedReportTaskResponse errorReport =
                        new PostedReportTaskResponse("UNKNOWN", LocalDate.now(), false);
                    errorReport.setMarkedPostedCount(0);
                    errorReport.markAsFailed(
                        "An error occurred when processing reports from SFTP server: " + e.getMessage());
                    responseList.add(errorReport);
                }
            });

        }
        return responseList;
    }

    /**
     * Marks the letter as posted in the database.
     *
     * @param letterPrintStatus The letter print status
     * @param reportFileName    The report file name
     */
    private boolean markAsPosted(LetterPrintStatus letterPrintStatus, String reportFileName) {
        final AtomicBoolean markedAsPosted = new AtomicBoolean(false);
        dataAccessService.findLetterStatus(letterPrintStatus.id).ifPresentOrElse(status -> {
            if (status.equals(LetterStatus.Uploaded)) {
                dataAccessService.markLetterAsPosted(
                    letterPrintStatus.id,
                    letterPrintStatus.printedAt.toLocalDateTime()
                );
                markedAsPosted.set(true);
                logger.info("Marked letter {} as posted", letterPrintStatus.id);
            } else {
                logger.warn("Failed to mark letter {} as posted - unexpected status: {}. Report file name: {}",
                    letterPrintStatus.id, status, reportFileName);
            }
        }, () -> logger.error("Failed to mark letter {} as posted - unknown letter. Report file name: {}",
            letterPrintStatus.id, reportFileName));
        return markedAsPosted.get();
    }

    private ReportInfo extractReportInfoFromParsedReport(ParsedReport parsedReport) {

        // the ideal is that we can simply extract the report info from the
        // report filename, though we do need to ensure that extracted report
        // name is known/expected, and we need all three elements before
        // proceeding.
        Optional<String> reportCode = calculateReportCodeFromReportPath(parsedReport.path);
        Optional<Boolean> isInternational = calculateIsInternationalFromReportPath(parsedReport.path);
        LocalDate reportDate = calculateDateFromReport(parsedReport);

        // if we've got the lot, we can call it here
        if (reportCode.isPresent() && isInternational.isPresent()) {
            return new ReportInfo(reportCode.get(), reportDate, isInternational.get());
        }

        // if we couldn't extract the required data from the filename, then we
        // need to start looking into the associated letter records
        for (LetterPrintStatus lps : parsedReport.statuses) {
            // initially, we just need to find a letter that exists
            try {
                Optional<String> service = dataAccessService.findLetterService(lps.id);
                if (service.isPresent()) {
                    // then use that letter, and potentially it's status to look up the right code
                    uk.gov.hmcts.reform.sendletter.model.out.LetterStatus status =
                        letterService.getStatus(lps.id, Boolean.TRUE.toString(), Boolean.FALSE.toString());
                    // if we've already extracted the report code and/or international
                    // flag from the filename, then we'll use those as a priority.
                    String code = reportCode.orElseGet(() -> reportsServiceConfig.getReportCode(service.get(), status));
                    if (code != null) {
                        boolean international = isInternational.orElseGet(
                            () -> Optional.ofNullable(status.additionalData)
                                .map(m -> m.get("isInternational"))
                                .map(Object::toString) // probably unnecessary
                                .map(Boolean::valueOf)
                                .orElse(false));
                        return new ReportInfo(code, reportDate, international);
                    }
                }
            } catch (LetterNotFoundException e) {
                logger.warn("Letter not found for id '{}' during report code lookup", lps.id);
            }
        }

        return null;
    }

    /**
     * Calculates the internation status from the report path.
     *
     * <p>This returns an {@link Optional} {@link Boolean} as the lack of either the "international" string isn't
     * enough evidence that the report was "domestic".
     *
     * @param path the report path
     * @return an {@link Optional} {@link Boolean} which, if present, indicates whether the report is international
     *         or domestic. If empty, then an empirical determination couldn't be made.
     */
    private Optional<Boolean> calculateIsInternationalFromReportPath(final String path) {
        String pathLc = path.toLowerCase();
        if (pathLc.contains("international")) {
            return Optional.of(Boolean.TRUE);
        } else if (pathLc.contains("domestic")) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }

    /**
     * Attempts to calculate the report code from the report path.
     *
     * @param reportPath the filename of the report
     * @return An {@link Optional} containing the code, if located
     */
    private Optional<String> calculateReportCodeFromReportPath(final String reportPath) {
        Matcher matcher = REPORT_CODE_PATTERN.matcher(reportPath.toUpperCase());
        if (matcher.find()) {
            String reportCode = matcher.group();
            if (reportsServiceConfig.getReportCodes().contains(reportCode)) {
                return Optional.of(reportCode);
            }
        }
        logger.info("Could not determine report code from report filename: {}", reportPath);
        return Optional.empty();
    }

    /**
     * Calculate the report date from the parsed report, preferring the date of the file to the date in the
     * {@link ParsedReport}.
     *
     * @param parsedReport the parsed report
     * @return A {@link LocalDate}
     */
    private LocalDate calculateDateFromReport(ParsedReport parsedReport) {
        Matcher matcher = DATE_PATTERN.matcher(parsedReport.path);
        if (matcher.find()) {
            String dateString = matcher.group();
            try {
                return LocalDate.parse(dateString, dateString.charAt(4) == '-'
                    ? DateTimeFormatter.ISO_LOCAL_DATE
                    : REPORT_DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                logger.warn("Could not parse date from report path '{}'", parsedReport.path);
            }
        }
        return parsedReport.reportDate;
    }
}
