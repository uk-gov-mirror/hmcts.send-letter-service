package uk.gov.hmcts.reform.sendletter.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.logging.appinsights.AbstractAppInsights;
import uk.gov.hmcts.reform.sendletter.entity.Letter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static java.time.temporal.ChronoUnit.MILLIS;

@Aspect
@Component
public class AppInsights extends AbstractAppInsights {

    static final String LETTER_NOT_PRINTED = "LetterNotPrinted";

    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AppInsights(TelemetryClient telemetry) {
        super(telemetry);
    }

    // dependencies

    @Pointcut("@annotation(dependency)")
    public void dependencyPointCut(Dependency dependency) {
        // point cut definition
    }

    @Around("dependencyPointCut(dependency)")
    public Object trackDependency(ProceedingJoinPoint joinPoint, Dependency dependency) throws Throwable {
        Instant start = Instant.now();

        try {
            Object proceed = joinPoint.proceed();

            telemetry.trackDependency(
                dependency.value(),
                dependency.command(),
                new Duration(MILLIS.between(start, Instant.now())),
                true
            );

            return proceed;
        } catch (Throwable exception) {
            telemetry.trackDependency(
                dependency.value(),
                dependency.command(),
                new Duration(MILLIS.between(start, Instant.now())),
                false
            );
            telemetry.trackException((Exception) exception);

            throw exception;
        }
    }

    // events

    public void trackStaleLetter(Letter staleLetter) {
        LocalDateTime sentToPrint = LocalDateTime.ofInstant(staleLetter.getSentToPrintAt().toInstant(), ZoneOffset.UTC);
        Map<String, String> properties = new HashMap<>();

        properties.put("letterId", staleLetter.getId().toString());
        properties.put("messageId", staleLetter.getMessageId());
        properties.put("service", staleLetter.getService());
        properties.put("type", staleLetter.getType());
        properties.put("sentToPrintDayOfWeek", sentToPrint.getDayOfWeek().name());
        properties.put("sentToPrintAt", sentToPrint.format(TIME_FORMAT));

        telemetry.trackEvent(LETTER_NOT_PRINTED, properties, null);
    }

    public void trackException(Exception exception) {
        telemetry.trackException(exception);
    }
}
