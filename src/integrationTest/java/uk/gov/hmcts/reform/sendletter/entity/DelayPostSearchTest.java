package uk.gov.hmcts.reform.sendletter.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Posted;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class DelayPostSearchTest {

    @Autowired
    LetterRepository letterRepository;

    @BeforeEach
    void setUp() {
        letterRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        letterRepository.deleteAll();
    }

    @Test
    void should_return_delayed_print_records() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        storeLetter(Posted, "type-1", currentDateTime.minusDays(4), currentDateTime.minusDays(1));
        storeLetter(Posted, "type-2",  currentDateTime.minusDays(5), currentDateTime.minusDays(1));
        storeLetter(Posted, "type-2",  currentDateTime.minusDays(4), currentDateTime.minusDays(4).plusHours(2));
        storeLetter(Posted, "type-2",  currentDateTime.minusDays(3), currentDateTime.minusDays(1));
        storeLetter(Posted, "type-2",  currentDateTime.minusDays(3), currentDateTime.minusDays(1).plusHours(1));

        try (Stream<Letter> deplayedPostedLetter = letterRepository.findDeplayedPostedLetter(
                currentDateTime.minusDays(6), currentDateTime, 48)) {
            assertThat(deplayedPostedLetter.count()).isEqualTo(3);
        }
    }

    private void storeLetter(LetterStatus status, String type, LocalDateTime createdAt, LocalDateTime postedAt) {
        Letter letter = SampleData.letterEntity("service1", createdAt, type);
        letter.setStatus(status);
        letter.setSentToPrintAt(createdAt.plusMinutes(10));
        letter.setPrintedAt(postedAt);
        letterRepository.save(letter);
    }
}
