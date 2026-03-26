package uk.gov.hmcts.reform.sendletter.services;

import com.google.common.collect.Sets;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Utility for filling zero rows.
 */
@Component
@EnableConfigurationProperties(ReportsServiceConfig.class)
public class ZeroRowFiller {

    private final Map<String, String> reportsServiceConfig;

    /**
     * Constructor for the ZeroRowFiller.
     *
     * @param reportsServiceConfig The configuration for reports service
     */
    public ZeroRowFiller(ReportsServiceConfig reportsServiceConfig) {
        this.reportsServiceConfig = reportsServiceConfig.getServiceDisplayNameMap();
    }

    /**
     * Fill the list with zero rows.
     *
     * @param listToFill The list to fill
     * @return The list filled with zero rows
     */
    public List<LettersCountSummary> fill(List<LettersCountSummary> listToFill) {
        return Stream.concat(
            listToFill.stream(),
            missingServiceNames(listToFill).stream().map(
                serviceName -> new LettersCountSummary(serviceName, 0)
            )
        ).collect(toList());
    }

    /**
     * Get the missing service names.
     *
     * @param listToFill The list to fill
     * @return The missing service names
     */
    private Set<String> missingServiceNames(List<LettersCountSummary> listToFill) {
        return Sets.difference(
            Sets.newHashSet(reportsServiceConfig.values()), //All service names
            listToFill.stream().map(res -> res.serviceName).collect(toSet())
        );
    }
}

