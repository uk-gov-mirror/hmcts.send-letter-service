package uk.gov.hmcts.reform.sendletter.tasks.reports;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.services.ReportsService;
import uk.gov.hmcts.reform.sendletter.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnBean(EmailSender.class)
@ConditionalOnProperty(prefix = "reports.upload-summary", name = "enabled")
public class DailyLetterUploadSummaryReport {

    private static final Logger log = LoggerFactory.getLogger(DailyLetterUploadSummaryReport.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String EMAIL_SUBJECT = "Sent Letters Summary split by services";
    private static final String ATTACHMENT_PREFIX = "send-letter-summary-";

    private final ReportsService reportService;

    private final EmailSender emailSender;

    private final String[] recipients;

    public DailyLetterUploadSummaryReport(
        ReportsService reportService,
        EmailSender emailSender,
        @Value("${reports.upload-summary.recipients}") String[] recipients
    ) {
        this.reportService = reportService;
        this.emailSender = emailSender;

        if (recipients == null) {
            this.recipients = new String[0];
        } else {
            this.recipients = Arrays.copyOf(recipients, recipients.length);
        }

        if (this.recipients.length == 0) {
            log.error("No recipients configured");
        }
    }

    @SchedulerLock(name = "daily-letter-upload-summary", lockAtLeastFor = 5_000)
    @Scheduled(cron = "${reports.upload-summary.cron}", zone = EUROPE_LONDON)
    public void send() {
        if (recipients.length == 0) {
            log.error("Not sending a report. No recipients configured");
        } else {
            log.info("Generating report");
            LocalDate today = LocalDate.now();

            try {
                List<LettersCountSummary> summary = reportService.getCountFor(today);
                File csv = CsvWriter.writeLettersCountSummaryToCsv(summary);
                Attachment attachment = new Attachment(ATTACHMENT_PREFIX + today.format(FORMATTER) + ".csv", csv);

                emailSender.send(EMAIL_SUBJECT, recipients, attachment);

                log.info("Report sent");
            } catch (IOException exception) {
                log.error("Unable to generate report", exception);
            }
        }
    }
}
