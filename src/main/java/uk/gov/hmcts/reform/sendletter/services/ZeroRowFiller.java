package uk.gov.hmcts.reform.sendletter.services;

import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class ZeroRowFiller {

    private final List<String> serviceFolders;

    public ZeroRowFiller(ServiceFolderMapping serviceFolderMapping) {
        this.serviceFolders = Lists.newArrayList(serviceFolderMapping.getFolders());
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
            Sets.newHashSet(this.serviceFolders),
            listToFill.stream().map(res -> res.service).collect(toSet())
        );
    }
}

