package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.logging.appinsights.AbstractAppInsights;

import java.util.Collections;
import java.util.Map;

@Component
public class AppInsights extends AbstractAppInsights {

    static final String SERVICE_BUS_DEPENDENCY = "ServiceBus";
    static final String SERVICE_BUS_MESSAGE_ACKNOWLEDGED = "MessageAcknowledged";
    static final String MESSAGE_RECEIVED = "MessageReceived";

    public AppInsights(TelemetryClient telemetry) {
        super(telemetry);
    }

    public void trackMessageReceived(String serviceName, String template, String messageId) {
        Map<String, String> properties = ImmutableMap.of(
            "service", serviceName,
            "template", template,
            "messageId", messageId
        );

        telemetry.trackEvent(MESSAGE_RECEIVED, properties, null);
    }

    public void trackMessageAcknowledgement(java.time.Duration duration, boolean success, String messageId) {
        telemetry.trackDependency(
            SERVICE_BUS_DEPENDENCY,
            SERVICE_BUS_MESSAGE_ACKNOWLEDGED,
            new Duration(duration.toMillis()),
            success
        );

        if (success) {
            telemetry.trackEvent(
                SERVICE_BUS_MESSAGE_ACKNOWLEDGED,
                Collections.singletonMap("messageId", messageId),
                null
            );
        }
    }

    public void trackException(Exception exception) {
        telemetry.trackException(exception);
    }
}