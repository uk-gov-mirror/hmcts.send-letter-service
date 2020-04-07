package uk.gov.hmcts.reform.sendletter.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Posted;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Uploaded;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class LettersFromGivenDaySearchTest {

    private static final String SMOKE_TEST_LETTER_TYPE = "smoke_test";

    @Autowired
    private LetterRepository repository;

    @Test
    void should_read_expected_data_from_db() {
        // given
        Letter letter = SampleData.letterEntity("service1", now(), "some_type");
        letter.setStatus(Posted);
        letter.setPrintedAt(now().plusDays(1));
        letter.setSentToPrintAt(now().minusDays(2));

        repository.save(letter);

        // when
        List<BasicLetterInfo> letters = repository.findCreatedAt(LocalDate.now());

        // then
        assertThat(letters).hasSize(1);
        assertThat(letters.get(0))
            .satisfies(l -> {
                assertThat(l.getCreatedAt()).isEqualTo(letter.getCreatedAt());
                assertThat(l.getSentToPrintAt()).isEqualTo(letter.getSentToPrintAt());
                assertThat(l.getPrintedAt()).isEqualTo(letter.getPrintedAt());
                assertThat(l.getService()).isEqualTo(letter.getService());
                assertThat(l.getType()).isEqualTo(letter.getType());
                assertThat(l.getStatus()).isEqualTo(letter.getStatus().toString());
            });
    }

    @Test
    void should_exclude_letters_created_on_different_day() {
        // given
        storeLetter(Uploaded, "type-1", now());
        storeLetter(Posted, "type-2", now());
        storeLetter(Uploaded, "type-1", now().minusDays(1));
        storeLetter(Posted, "type-2", now().plusDays(1));

        // when
        List<BasicLetterInfo> letters = repository.findCreatedAt(LocalDate.now());

        // then
        assertThat(letters).hasSize(2);
    }

    @Test
    public void should_exclude_smoke_test_letters() {
        // given
        storeLetter(Uploaded, "type-1", now());
        storeLetter(Posted, "type-2", now());
        storeLetter(Posted, SMOKE_TEST_LETTER_TYPE, now());
        storeLetter(Posted, SMOKE_TEST_LETTER_TYPE, now());

        // when
        List<BasicLetterInfo> letters = repository.findCreatedAt(LocalDate.now());

        // then
        assertThat(letters).hasSize(2);
    }

    private void storeLetter(LetterStatus status, String type, LocalDateTime createdAt) {
        Letter letter = SampleData.letterEntity("service1", createdAt, type);
        letter.setStatus(status);
        repository.save(letter);
    }
}
