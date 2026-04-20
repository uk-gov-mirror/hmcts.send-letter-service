package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.entity.LettersCountSummaryRepository;
import uk.gov.hmcts.reform.sendletter.entity.ReportRepository;
import uk.gov.hmcts.reform.sendletter.entity.reports.ServiceLettersCountSummary;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.model.out.MissingReportsResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.localDateTimeWithUtc;

/**
 * Service to handle reports.
 */
@Service
public class ReportsService {

    private static final String TEST_SERVICE = "Bulk Print";

    private final LettersCountSummaryRepository repo;

    private final ReportRepository reportRepository;

    private final ReportsServiceConfig reportsServiceConfig;

    private final ZeroRowFiller zeroRowFiller;

    private final String timeFromHour;

    private final String timeToHour;

    /**
     * Constructor for the ReportsService.
     *
     * @param repo The repository for letters count summary
     * @param reportRepository The repository for reports
     * @param reportsServiceConfig The configuration for reports service
     * @param zeroRowFiller The utility for filling zero rows
     * @param downtimeFromHour The downtime from hour
     * @param downtimeToHour The downtime to hour
     */
    public ReportsService(
        LettersCountSummaryRepository repo,
        ReportRepository reportRepository,
        ReportsServiceConfig reportsServiceConfig,
        ZeroRowFiller zeroRowFiller,
        @Value("${ftp.downtime.from}") String downtimeFromHour,
        @Value("${ftp.downtime.to}") String downtimeToHour
    ) {
        this.repo = repo;
        this.reportRepository = reportRepository;
        this.reportsServiceConfig = reportsServiceConfig;
        this.zeroRowFiller = zeroRowFiller;
        this.timeFromHour = downtimeToHour;
        this.timeToHour = downtimeFromHour;
    }

    /**
     * Get count for a date.
     *
     * @param date the date
     * @return the count for the date
     */
    public List<LettersCountSummary> getCountFor(LocalDate date) {
        LocalDateTime dateTimeFrom = localDateTimeWithUtc(date.minusDays(1), LocalTime.parse(timeFromHour));
        LocalDateTime dateTimeTo = localDateTimeWithUtc(date, LocalTime.parse(timeToHour));

        return zeroRowFiller.fill(
            repo.countByDate(dateTimeFrom, dateTimeTo).stream().map(this::fromDb).collect(toList()))
            .stream()
            .filter(
                summary -> isNotBlank(summary.serviceName) && !summary.serviceName.equals(TEST_SERVICE)
            ) //excludes nulls, empty values and test service
            .collect(toList());
    }

    public List<MissingReportsResponse> checkReports(
        LocalDate startDate,
        LocalDate endDate
    ) {
        Set<String> expectedCodes = reportsServiceConfig.getReportCodes();
        Set<String> presentReports = reportRepository.findAllByReportDateBetweenAndReportCodeIn(
            startDate, endDate, expectedCodes.stream().toList())
                .stream()
                .map(r -> r.getReportDate() + r.getReportCode() + r.isInternational())
                .collect(Collectors.toSet());

        List<MissingReportsResponse> missingReports = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (String code : expectedCodes) {
                if (!presentReports.contains(date.toString() + code + "false")) {
                    missingReports.add(new MissingReportsResponse(code, false, date));
                }
                if (!presentReports.contains(date.toString() + code + "true")) {
                    missingReports.add(new MissingReportsResponse(code, true, date));
                }
            }
        }
        return missingReports;
    }

    /**
     * Get count for a date range.
     *
     * @param dbSummary the db summary
     * @return letters count summary
     */
    private LettersCountSummary fromDb(ServiceLettersCountSummary dbSummary) {
        return new LettersCountSummary(
            reportsServiceConfig.getDisplayName(dbSummary.getService()).orElse(dbSummary.getService()),
            dbSummary.getUploaded());
    }
}
