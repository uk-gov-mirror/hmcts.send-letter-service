package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.BooleanUtils;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import com.microsoft.applicationinsights.web.internal.correlation.TelemetryCorrelationUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.reform.sendletter.util.TimeZones.getCurrentEuropeLondonInstant;

@Aspect
@Component
public class AppInsights {

    private static final Logger log = LoggerFactory.getLogger(AppInsights.class);

    static final String LETTER_NOT_PRINTED = "LetterNotPrinted";

    static final String LETTER_PRINT_REPORT = "LetterPrintReportReceived";

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

    @Pointcut("@annotation(dependency)")
    public void useAdviceOnDependency(Dependency dependency) {
        // empty pointcut definition
    }

    @Around("useAdviceOnDependency(dependency)")
    public Object aroundDependency(ProceedingJoinPoint joinPoint, Dependency dependency) throws Throwable {
        Instant start = Instant.now();
        boolean success = false;

        try {
            Object object = joinPoint.proceed();

            success = true;

            return object;
        } finally {
            handleDependencyTelemetry(dependency, ChronoUnit.MILLIS.between(start, Instant.now()), success);
        }
    }

    private void handleDependencyTelemetry(Dependency dependency, long durationInMillis, boolean success) {
        // dependency definition
        RemoteDependencyTelemetry dependencyTelemetry = new RemoteDependencyTelemetry(
            dependency.name(),
            dependency.command(),
            new Duration(durationInMillis),
            success
        );

        dependencyTelemetry.setType(dependency.type());

        // tracing support
        RequestTelemetryContext context = ThreadContext.getRequestTelemetryContext();

        if (context != null) {
            RequestTelemetry requestTelemetry = context.getHttpRequestTelemetry();
            dependencyTelemetry.setId(TelemetryCorrelationUtils.generateChildDependencyId());
            dependencyTelemetry.getContext().getOperation().setId(
                requestTelemetry.getContext().getOperation().getId()
            );
            dependencyTelemetry.getContext().getOperation().setParentId(
                requestTelemetry.getId()
            );
        }

        telemetryClient.trackDependency(dependencyTelemetry);
    }

    // events

    public void trackStaleLetter(BasicLetterInfo staleLetter) {
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
}
