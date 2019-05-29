package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.BooleanUtils;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.reform.sendletter.util.TimeZones.getCurrentEuropeLondonInstant;

@Aspect
@Component
public class AppInsights {

    private static final Logger log = LoggerFactory.getLogger(AppInsights.class);

    static final String FTP_TYPE = "FTP";

    static final String LETTER_NOT_PRINTED = "LetterNotPrinted";

    static final String LETTER_PRINT_REPORT = "LetterPrintReportReceived";

    static final String LETTER_UPLOAD_FOR_PRINT = "LetterUploadedForPrint";

    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TelemetryClient telemetryClient;

    public AppInsights(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
    }

    // schedules

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void aroundSchedule(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestTelemetryContext requestTelemetry = ThreadContext.getRequestTelemetryContext();
        boolean success = false;

        try {
            joinPoint.proceed();

            success = true;
        } finally {
            handleRequestTelemetry(requestTelemetry, joinPoint.getTarget().getClass().getSimpleName(), success);
        }
    }

    private void handleRequestTelemetry(
        RequestTelemetryContext requestTelemetryContext,
        String caller,
        boolean success
    ) {
        String requestName = "Schedule /" + caller;

        if (requestTelemetryContext != null) {
            handleRequestTelemetry(
                requestTelemetryContext.getHttpRequestTelemetry(),
                requestName,
                requestTelemetryContext.getRequestStartTimeTicks(),
                success
            );
        } else {
            log.warn(
                "Request Telemetry Context has been removed by ThreadContext - cannot log '{}' request",
                requestName
            );
        }
    }

    private void handleRequestTelemetry(
        RequestTelemetry requestTelemetry,
        String requestName,
        long start,
        boolean success
    ) {
        if (requestTelemetry != null) {
            requestTelemetry.setName(requestName);
            requestTelemetry.setDuration(new Duration(getCurrentEuropeLondonInstant().toEpochMilli() - start));
            requestTelemetry.setSuccess(success);

            telemetryClient.trackRequest(requestTelemetry);
        }
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
        telemetryClient.trackDependency(dependencyTelemetry);
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

        telemetryClient.trackEvent(LETTER_NOT_PRINTED, properties, null);
    }

    public void trackPrintReportReceived(ParsedReport report) {
        telemetryClient.trackEvent(
            LETTER_PRINT_REPORT,
            ImmutableMap.of("isReportParsedFully", BooleanUtils.toStringYesNo(report.allRowsParsed)),
            ImmutableMap.of("reportSize", (double) report.statuses.size())
        );
    }

    // metrics

    public void trackUploadedLetters(int lettersUploaded) {
        telemetryClient.trackMetric(LETTER_UPLOAD_FOR_PRINT, lettersUploaded);
    }
}
