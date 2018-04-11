package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sendletter.util.MessageIdProvider.randomMessageId;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class StaleLettersTaskTest {

    @Autowired
    private LetterRepository repository;

    private LocalTime cutOff;

    private LocalTime secondBeforeCutOff;

    @Mock
    private AppInsights insights;

    private StaleLettersTask task;

    @Before
    public void setUp() {
        FtpAvailabilityChecker checker = new FtpAvailabilityChecker("13:00", "14:00");
        task = new StaleLettersTask(repository, insights, checker);
        cutOff = checker.getDowntimeStart();
        secondBeforeCutOff = cutOff.minusSeconds(1);

        // making sure each test has fresh table
        repository.deleteAll();
    }

    @Test
    public void should_do_nothing_when_there_are_no_unprinted_letters() {
        // when
        task.run();

        // then
        verify(insights, never()).trackStaleLetter(any(Letter.class));
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    public void should_report_to_insights_when_there_is_an_unprinted_letter() {
        // given
        Letter letter = createLetterWithSentToPrintTime(secondBeforeCutOff);

        ArgumentCaptor<Letter> captor = ArgumentCaptor.forClass(Letter.class);

        // when
        task.run();

        // then
        verify(insights).trackStaleLetter(captor.capture());

        // and
        assertThat(captor.getAllValues()).hasSize(1);
        assertThat(captor.getValue().getId()).isEqualTo(letter.getId());
    }

    @Test
    public void should_not_pick_up_letter_if_sent_to_print_happened_at_the_cutoff_and_later() {
        // given
        createLetterWithSentToPrintTime(cutOff);
        createLetterWithSentToPrintTime(cutOff.plusSeconds(1));

        // when
        task.run();

        // then
        verify(insights, never()).trackStaleLetter(any(Letter.class));
    }

    @Test
    public void should_not_pick_up_letter_if_not_sent_to_print() {
        // given
        createLetterWithSentToPrintTime(null);

        // when
        task.run();

        // then
        verify(insights, never()).trackStaleLetter(any(Letter.class));
    }

    @Test
    public void should_not_pick_up_letter_if_it_is_already_printed() {
        // given
        Letter letter = createLetterWithSentToPrintTime(secondBeforeCutOff);
        letter.setStatus(LetterStatus.Posted);
        letter.setPrintedAt(Timestamp.from(Instant.now()));

        // when
        repository.save(letter);
        task.run();

        // then
        verify(insights, never()).trackStaleLetter(any(Letter.class));
    }

    private Letter createLetterWithSentToPrintTime(LocalTime sentToPrintAtTime) {
        Letter letter = new Letter(UUID.randomUUID(), randomMessageId(), "service", null, "type", null, false);

        if (sentToPrintAtTime != null) {
            letter.setStatus(LetterStatus.Uploaded);
            letter.setSentToPrintAt(
                Timestamp.valueOf(LocalDateTime.now()
                    .minusDays(1)
                    .with(sentToPrintAtTime)
                )
            );
        }

        return repository.save(letter);
    }
}
