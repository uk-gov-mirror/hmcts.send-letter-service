package uk.gov.hmcts.reform.sendletter.tasks.alerts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.StaleLetterService;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleLettersTaskTest {

    @Mock
    private StaleLetterService staleLetterService;

    @Mock
    private AppInsights insights;

    private StaleLettersTask task;

    @BeforeEach
    void setUp() {
        task = new StaleLettersTask(staleLetterService, insights);
    }

    @Test
    void should_do_nothing_when_there_are_no_unprinted_letters() {
        // given
        given(staleLetterService.getStaleLetters()).willReturn(emptyList());

        // when
        task.run();

        // then
        verify(insights, never()).trackStaleLetter(any(BasicLetterInfo.class));
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void should_report_to_insights_when_there_is_an_unprinted_letter() {
        // given
        BasicLetterInfo letter = staleLetter();

        given(staleLetterService.getStaleLetters()).willReturn(asList(letter));

        // when
        task.run();

        // then
        ArgumentCaptor<BasicLetterInfo> captor = ArgumentCaptor.forClass(BasicLetterInfo.class);
        verify(insights).trackStaleLetter(captor.capture());

        // and
        assertThat(captor.getAllValues()).hasSize(1);
        assertThat(captor.getValue().getId()).isEqualTo(letter.getId());
    }

    private BasicLetterInfo staleLetter() {
        BasicLetterInfo letter = mock(BasicLetterInfo.class);
        when(letter.getId()).thenReturn(UUID.randomUUID());

        return letter;
    }
}
