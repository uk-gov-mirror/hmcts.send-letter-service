package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "reports")
public class ReportsServiceConfig {

    private Map<String, String> serviceConfig;

    public static class Mapping {

        private String service;
        private String displayName;

        public Mapping() {
            // Spring needs it.
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    public Map<String, String> getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(Map<String, String> serviceConfig) {
        this.serviceConfig = serviceConfig;
    }
}
