package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.BooleanUtils;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class AppInsights {

    static final String FTP_TYPE = "FTP";

    static final String LETTER_NOT_PRINTED = "LetterNotPrinted";

    static final String LETTER_PRINT_REPORT = "LetterPrintReportReceived";

    static final String LETTER_UPLOAD_FOR_PRINT = "LetterUploadedForPrint";

    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TelemetryClient telemetry;

    public AppInsights(TelemetryClient telemetry) {
        this.telemetry = telemetry;
    }

    // dependencies

    private void trackDependency(String command, java.time.Duration duration, boolean success) {
        RemoteDependencyTelemetry dependencyTelemetry = new RemoteDependencyTelemetry(
            AppDependency.FTP_CLIENT,
            command,
            new Duration(duration.toMillis()),
            success
        );
        dependencyTelemetry.setType(FTP_TYPE);
        telemetry.trackDependency(dependencyTelemetry);
    }

    public void trackFtpUpload(java.time.Duration duration, boolean success) {
        trackDependency(AppDependencyCommand.FTP_FILE_UPLOADED, duration, success);
    }

    public void trackFtpReportDeletion(java.time.Duration duration, boolean success) {
        trackDependency(AppDependencyCommand.FTP_REPORT_DELETED, duration, success);
    }

    public void trackFtpReportDownload(java.time.Duration duration, boolean success) {
        trackDependency(AppDependencyCommand.FTP_REPORT_DOWNLOADED, duration, success);
    }

    // events

    public void trackStaleLetter(Letter staleLetter) {
        Map<String, String> properties = new HashMap<>();

        properties.put("letterId", staleLetter.getId().toString());
        properties.put("checksum", staleLetter.getChecksum());
        properties.put("service", staleLetter.getService());
        properties.put("type", staleLetter.getType());
        properties.put("sentToPrintDayOfWeek", staleLetter.getSentToPrintAt().getDayOfWeek().name());
        properties.put("sentToPrintAt", staleLetter.getSentToPrintAt().format(TIME_FORMAT));

        telemetry.trackEvent(LETTER_NOT_PRINTED, properties, null);
    }

    public void trackPrintReportReceived(ParsedReport report) {
        telemetry.trackEvent(
            LETTER_PRINT_REPORT,
            ImmutableMap.of("isReportParsedFully", BooleanUtils.toStringYesNo(report.allRowsParsed)),
            ImmutableMap.of("reportSize", (double) report.statuses.size())
        );
    }

    // metrics

    public void trackUploadedLetters(int lettersUploaded) {
        telemetry.trackMetric(LETTER_UPLOAD_FOR_PRINT, lettersUploaded);
    }
}
