package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

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

    public void setServiceConfig(List<Mapping> mappings) {
        this.serviceConfig = mappings
            .stream()
            .collect(toMap(Mapping::getService, Mapping::getDisplayName));
    }

    public Optional<String> getDisplayName(String serviceName) {
        return Optional.ofNullable(serviceConfig.get(serviceName));
    }
}
