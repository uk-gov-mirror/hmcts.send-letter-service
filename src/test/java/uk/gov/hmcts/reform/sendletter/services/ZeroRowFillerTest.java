package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ZeroRowFillerTest {

    @Mock
    private ServiceFolderMapping serviceFolderMapping;

    @Test
    void should_add_missing_zero_row_when_needed() {
        //given
        given(serviceFolderMapping.getFolders()).willReturn(Arrays.asList("ServiceA", "ServiceB", "ServiceC"));

        ZeroRowFiller zeroRowFiller = new ZeroRowFiller(serviceFolderMapping);
        List<LettersCountSummary> listToFill = asList(
            new LettersCountSummary("ServiceA", 10),
            new LettersCountSummary("ServiceB", 15)
        );

        //when
        List<LettersCountSummary> result = zeroRowFiller.fill(listToFill);

        //then
        assertThat(result)
            .isNotEmpty()
            .hasSize(3)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new LettersCountSummary("ServiceA", 10),
                new LettersCountSummary("ServiceB", 15),
                new LettersCountSummary("ServiceC", 0)
            );
    }

    @Test
    void should_add_all_services_when_input_collection_is_empty() {
        //given
        given(serviceFolderMapping.getFolders()).willReturn(Arrays.asList("ServiceA", "ServiceB", "ServiceC"));

        ZeroRowFiller zeroRowFiller = new ZeroRowFiller(serviceFolderMapping);

        //when
        List<LettersCountSummary> result = zeroRowFiller.fill(Collections.emptyList());

        //then
        assertThat(result)
            .isNotEmpty()
            .hasSize(3)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new LettersCountSummary("ServiceA", 0),
                new LettersCountSummary("ServiceB", 0),
                new LettersCountSummary("ServiceC", 0)
            );
    }

    @Test
    void should_not_change_the_collection_when_all_services_exist() {
        //given
        given(serviceFolderMapping.getFolders()).willReturn(Arrays.asList("ServiceA", "ServiceB", "ServiceC"));

        ZeroRowFiller zeroRowFiller = new ZeroRowFiller(serviceFolderMapping);
        List<LettersCountSummary> listToFill = asList(
            new LettersCountSummary("ServiceA", 10),
            new LettersCountSummary("ServiceB", 15),
            new LettersCountSummary("ServiceC", 1)
        );

        //when
        List<LettersCountSummary> result = zeroRowFiller.fill(listToFill);

        //then
        assertThat(result)
            .isNotEmpty()
            .hasSize(3)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new LettersCountSummary("ServiceA", 10),
                new LettersCountSummary("ServiceB", 15),
                new LettersCountSummary("ServiceC", 1)
            );
    }
}
