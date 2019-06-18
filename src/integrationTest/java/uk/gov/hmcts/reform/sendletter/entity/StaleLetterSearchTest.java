package uk.gov.hmcts.reform.sendletter.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class StaleLetterSearchTest {

    private static final String SMOKE_TEST_LETTER_TYPE = "smoke_test";

    @Autowired
    private LetterRepository repository;

    @Test
    public void should_return_all_not_posted_letters_created_before_given_date() {
        // given
        LocalDateTime cutOffDate = LocalDateTime.now().minusDays(2);

        Letter createdLetter = storeLetter(cutOffDate.minusSeconds(1), LetterStatus.Created);
        Letter uploadedLetter = storeLetter(cutOffDate.minusDays(5), LetterStatus.Uploaded);

        // when
        List<BasicLetterInfo> letters = repository.findStaleLetters(cutOffDate);

        // then
        assertThat(letters.size()).isEqualTo(2);
        assertThat(letters.get(0).getId()).isEqualTo(uploadedLetter.getId());
        assertThat(letters.get(1).getId()).isEqualTo(createdLetter.getId());
    }

    @Test
    public void should_not_include_posted_and_aborted_letters_in_the_result() {
        // given
        LocalDateTime cutOffDate = LocalDateTime.now().minusDays(2);

        storeLetter(cutOffDate.minusSeconds(1), LetterStatus.Created);
        storeLetter(cutOffDate.minusSeconds(1), LetterStatus.Uploaded);
        Letter postedLetter = storeLetter(cutOffDate.minusSeconds(1), LetterStatus.Posted);
        Letter abortedLetter = storeLetter(cutOffDate.minusSeconds(1), LetterStatus.Aborted);

        // when
        List<BasicLetterInfo> letters = repository.findStaleLetters(cutOffDate);

        // then
        assertThat(letters.size()).isEqualTo(2);
        assertThat(letters).extracting(BasicLetterInfo::getId).doesNotContain(postedLetter.getId());
        assertThat(letters).extracting(BasicLetterInfo::getId).doesNotContain(abortedLetter.getId());
    }

    @Test
    public void should_not_include_letters_that_are_not_old_enough_in_the_result() {
        // given
        LocalDateTime cutOffDate = LocalDateTime.now().minusDays(2);

        storeLetter(cutOffDate, LetterStatus.Created);
        storeLetter(cutOffDate.plusSeconds(1), LetterStatus.Created);

        // when
        List<BasicLetterInfo> letters = repository.findStaleLetters(cutOffDate);

        // then
        assertThat(letters).isEmpty();
    }

    @Test
    public void should_not_include_smoke_test_letters_in_the_result() {
        // given
        LocalDateTime cutOffDate = LocalDateTime.now().minusDays(2);

        Letter smokeTestLetter = storeLetter(cutOffDate.minusDays(1), LetterStatus.Created, SMOKE_TEST_LETTER_TYPE);
        storeLetter(cutOffDate.minusDays(1), LetterStatus.Created, "not-smoke-test");

        // when
        List<BasicLetterInfo> letters = repository.findStaleLetters(cutOffDate);

        // then
        assertThat(letters.size()).isEqualTo(1);
        assertThat(letters.get(0).getId()).isNotEqualTo(smokeTestLetter.getId());
    }

    private Letter storeLetter(LocalDateTime createdAt, LetterStatus status) {
        return storeLetter(createdAt, status, "letterType1");
    }

    private Letter storeLetter(LocalDateTime createdAt, LetterStatus status, String type) {
        Letter letter = SampleData.letterEntity("service1", createdAt, type);
        letter.setStatus(status);
        return repository.save(letter);
    }
}
