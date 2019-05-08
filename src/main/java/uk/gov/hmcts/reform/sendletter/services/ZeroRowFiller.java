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

@Component
@EnableConfigurationProperties(ReportsServiceConfig.class)
public class ZeroRowFiller {

    private final Map<String, String> reportsServiceConfig;

    public ZeroRowFiller(ReportsServiceConfig reportsServiceConfig) {
        this.reportsServiceConfig = reportsServiceConfig.getServiceConfig();
    }

    public List<LettersCountSummary> fill(List<LettersCountSummary> listToFill) {
        return Stream.concat(
            listToFill.stream(),
            missingServiceFolders(listToFill).stream().map(
                serviceFolder -> new LettersCountSummary(serviceFolder, 0)
            )
        ).collect(toList());
    }

    private Set<String> missingServiceFolders(List<LettersCountSummary> listToFill) {
        return Sets.difference(
            Sets.newHashSet(reportsServiceConfig.values()), //All service names
            listToFill.stream().map(res -> res.serviceName).collect(toSet())
        );
    }
}

