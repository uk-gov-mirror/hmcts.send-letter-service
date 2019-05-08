package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;

@ConfigurationProperties(prefix = "reports")
public class ReportsServiceConfig {

    private Map<String, String> serviceConfig;

    public Map<String, String> getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(Map<String, String> serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public Optional<String> getDisplayName(String serviceName) {
        return Optional.ofNullable(serviceConfig.get(serviceName));
    }
}
