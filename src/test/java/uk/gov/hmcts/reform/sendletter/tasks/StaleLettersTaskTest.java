package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.StaleLetterService;

import java.util.UUID;
import java.util.stream.Stream;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
        given(staleLetterService.getStaleLetters()).willReturn(Stream.empty());

        // when
        task.run();

        // then
        verify(insights, never()).trackStaleLetter(any(Letter.class));
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    void should_report_to_insights_when_there_is_an_unprinted_letter() {
        // given
        Letter letter = staleLetter();

        given(staleLetterService.getStaleLetters()).willReturn(Stream.of(letter));

        // when
        task.run();

        // then
        ArgumentCaptor<Letter> captor = ArgumentCaptor.forClass(Letter.class);
        verify(insights).trackStaleLetter(captor.capture());

        // and
        assertThat(captor.getAllValues()).hasSize(1);
        assertThat(captor.getValue().getId()).isEqualTo(letter.getId());
    }

    private Letter staleLetter() {
        Letter letter = new Letter(
            UUID.randomUUID(),
            "checksum",
            "service",
            null,
            "type1",
            null,
            false,
            now()
        );

        letter.setStatus(LetterStatus.Uploaded);
        letter.setSentToPrintAt(now().minusDays(1));

        return letter;
    }
}
