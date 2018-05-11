package uk.gov.hmcts.reform.sendletter.config;

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
}
