package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration properties for reports service.
 */
@ConfigurationProperties(prefix = "reports")
public class ReportsServiceConfig {

    static final String SSCS_CODE = "SSCS";
    static final String SSCS_IB_SUFFIX = "-IB";
    static final String SSCS_REFORM_SUFFIX = "-REFORM";

    private Map<String, Mapping> serviceConfig;

    /**
     * Mapping class for service report config.
     */
    public static class Mapping {

        private String service;
        private String displayName;
        private String reportCode;

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

        public String getReportCode() {
            return reportCode;
        }

        public void setReportCode(String reportCode) {
            this.reportCode = reportCode;
        }

        public Mapping getSelf() {
            return this;
        }
    }

    public Map<String, Mapping> getServiceConfig() {
        return serviceConfig;
    }

    /**
     * Returns this configuration object as a simplified display name lookup.
     *
     * @return the {@link Map} of Service id to display name
     */
    public Map<String, String> getServiceDisplayNameMap() {
        return serviceConfig.values().stream()
            .collect(Collectors.toMap(
                ReportsServiceConfig.Mapping::getService,
                ReportsServiceConfig.Mapping::getDisplayName)
            );
    }

    /**
     * Return the set of distinct report codes in use by all services.
     *
     * @return the {@link Set} of report codes current in use.
     */
    public Set<String> getReportCodes() {
        Set<String> codes = serviceConfig.values().stream()
            .map(ReportsServiceConfig.Mapping::getReportCode)
            .collect(Collectors.toCollection(HashSet::new));

        // special case for SSCS - remove the simple code and
        // replace it with both suffixed versions
        if (codes.contains(SSCS_CODE)) {
            codes.remove(SSCS_CODE);
            codes.add(SSCS_CODE + SSCS_IB_SUFFIX);
            codes.add(SSCS_CODE + SSCS_REFORM_SUFFIX);
        }

        return codes;
    }

    /**
     * Set the service configuration.
     *
     * @param mappings The mappings
     */
    public void setServiceConfig(List<Mapping> mappings) {
        this.serviceConfig = mappings
            .stream()
            .collect(Collectors.toMap(Mapping::getService, Mapping::getSelf));
    }

    /**
     * Get the display name for a service.
     *
     * @param serviceName The name of the service
     * @return The display name
     */
    public Optional<String> getDisplayName(String serviceName) {
        return Optional.ofNullable(serviceConfig.get(serviceName)).map(Mapping::getDisplayName);
    }

    /**
     * Get the report code for a service.
     *
     * @param serviceName  The name of the service
     * @param letterStatus The associated letter status (only required for SSCS)
     * @return The report code (may be {@code null})
     */
    public String getReportCode(String serviceName, LetterStatus letterStatus) {
        String code = Optional.ofNullable(serviceName)
            .map(serviceConfig::get)
            .map(Mapping::getReportCode)
            .orElse(null);

        // special case for sscs
        if (SSCS_CODE.equalsIgnoreCase(code)) {
            code = Optional.ofNullable(letterStatus)
                .map(ls -> ls.additionalData)
                .map(ad -> ad.get("isIbca"))
                .map(Object::toString)// can be either a string or a boolean...
                .filter("true"::equalsIgnoreCase)
                .map(c -> SSCS_CODE + SSCS_IB_SUFFIX)
                .orElse(SSCS_CODE + SSCS_REFORM_SUFFIX);
        }

        return code;
    }
}
