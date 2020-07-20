package uk.gov.hmcts.reform.sendletter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintStatus;
import uk.gov.hmcts.reform.sendletter.services.LetterDataAccessService;
import uk.gov.hmcts.reform.sendletter.services.ReportParser;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.IFtpAvailabilityChecker;

import java.time.LocalTime;
import java.time.ZoneId;

import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

/**
 * Fetches reports from SFTP concerning posted
 * letters and sets status as Posted in the database.
 */
@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class MarkLettersPostedTask {

    private final LetterDataAccessService dataAccessService;
    private final FtpClient ftpClient;
    private final IFtpAvailabilityChecker ftpAvailabilityChecker;
    private final ReportParser parser;
    private final AppInsights insights;

    private static final Logger logger = LoggerFactory.getLogger(MarkLettersPostedTask.class);
    private static final String TASK_NAME = "MarkLettersPosted";

    public MarkLettersPostedTask(
        LetterDataAccessService dataAccessService,
        FtpClient ftp,
        IFtpAvailabilityChecker checker,
        ReportParser parser,
        AppInsights insights
    ) {
        this.dataAccessService = dataAccessService;
        this.ftpClient = ftp;
        this.ftpAvailabilityChecker = checker;
        this.parser = parser;
        this.insights = insights;
    }

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(cron = "${tasks.mark-letters-posted.cron}", zone = EUROPE_LONDON)
    public void run() {
        if (!ftpAvailabilityChecker.isFtpAvailable(LocalTime.now(ZoneId.of(EUROPE_LONDON)))) {
            logger.info("Not processing '{}' task due to FTP downtime window", TASK_NAME);
            return;
        }

        logger.info("Started '{}' task", TASK_NAME);
        try {
            ftpClient
                .downloadReports()
                .stream()
                .map(parser::parse)
                .forEach(parsedReport -> {
                    insights.trackPrintReportReceived(parsedReport);
                    logger.info(
                        "Updating letters from report {}. Letter count: {}",
                        parsedReport.path,
                        parsedReport.statuses.size()
                    );

                    parsedReport
                        .statuses
                        .forEach(status -> markAsPosted(status, parsedReport.path));

                    if (parsedReport.allRowsParsed) {
                        logger.info("Report {} successfully parsed, deleting", parsedReport.path);
                        ftpClient.deleteReport(parsedReport.path);
                    } else {
                        logger.warn("Report {} contained invalid rows, file not removed.", parsedReport.path);
                    }
                });

            logger.info("Completed '{}' task", TASK_NAME);
        } catch (Exception e) {
            logger.error("An error occurred when downloading reports from SFTP server", e);
        }
    }

    private void markAsPosted(LetterPrintStatus letterPrintStatus, String reportFileName) {
        dataAccessService
            .findLetterStatus(letterPrintStatus.id)
            .ifPresentOrElse(
                status -> {
                    if (status.equals(LetterStatus.Uploaded)) {
                        dataAccessService.markLetterAsPosted(
                            letterPrintStatus.id,
                            letterPrintStatus.printedAt.toLocalDateTime()
                        );
                        logger.info("Marked letter {} as posted", letterPrintStatus.id);
                    } else {
                        logger.warn(
                            "Failed to mark letter {} as posted - unexpected status: {}. Report file name: {}",
                            letterPrintStatus.id,
                            status,
                            reportFileName
                        );
                    }
                },
                () -> logger.error(
                    "Failed to mark letter {} as posted - unknown letter. Report file name: {}",
                    letterPrintStatus.id,
                    reportFileName
                )
            );
    }
}
