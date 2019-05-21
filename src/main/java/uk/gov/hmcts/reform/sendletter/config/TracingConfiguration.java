package uk.gov.hmcts.reform.sendletter.config;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.springframework.boot.actuate.trace.http.Include;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.api.filters.SensitiveHeadersRequestTraceFilter;

@Configuration
public class TracingConfiguration {

    @Bean
    public SensitiveHeadersRequestTraceFilter requestTraceFilter() {
        return new SensitiveHeadersRequestTraceFilter(Include.defaultIncludes());
    }

    /**
     * Custom {@link TelemetryProcessor} which amends the type of {@link RemoteDependencyTelemetry} before publishing.
     * Within 1h service consumes thousands of health related checks and therefore silences the actual dependencies.
     * We want to be able to recognise REAL dependency from those which are health related.
     */
    @Bean
    public TelemetryProcessor healthRecognitionProcessor() {
        return telemetry -> {
            String operationName = telemetry.getContext().getOperation().getName();

            if (telemetry instanceof RemoteDependencyTelemetry && operationName != null) {
                if (operationName.contains("/health")) {
                    ((RemoteDependencyTelemetry) telemetry).setType("Health");
                }
            }

            return true;
        };
    }
}
