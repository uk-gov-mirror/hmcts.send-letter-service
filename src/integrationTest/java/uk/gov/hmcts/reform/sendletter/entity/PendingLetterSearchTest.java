package uk.gov.hmcts.reform.sendletter.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Uploaded;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class PendingLetterSearchTest {

    private static final String SMOKE_TEST_LETTER_TYPE = "smoke_test";

    @Autowired
    private LetterRepository repository;

    @Test
    public void should_return_letters_in_created_status() {
        // given
        storeLetter(Uploaded, "type-1");
        storeLetter(Uploaded, "type-2");
        storeLetter(Created, "type-3");
        storeLetter(Created, "type-4");

        // when
        List<BasicLetterInfo> letters = repository.findPendingLetters();

        // then
        assertThat(letters)
            .extracting(l -> l.getType())
            .containsExactly("type-3", "type-4");
    }

    @Test
    public void should_not_include_smoke_test_letters_in_the_result() {
        // given
        storeLetter(Created, SMOKE_TEST_LETTER_TYPE);
        storeLetter(Created, "not-smoke-test-type-1");
        storeLetter(Created, "not-smoke-test-type-2");

        // when
        List<BasicLetterInfo> letters = repository.findPendingLetters();

        // then
        assertThat(letters.size()).isEqualTo(2);
        assertThat(letters).noneMatch(l -> l.getStatus().equals(SMOKE_TEST_LETTER_TYPE));
    }

    @Test
    public void should_set_properties_correctly() {
        // given
        Letter letter = SampleData.letterEntity("service", LocalDateTime.now(), "type", "fingerprint");
        letter.setStatus(Created);
        Letter savedLetter = repository.save(letter);

        // when
        List<BasicLetterInfo> letters = repository.findPendingLetters();

        // then
        assertThat(letters)
            .extracting(l -> tuple(
                l.getId(),
                l.getChecksum(),
                l.getCreatedAt(),
                l.getEncryptionKeyFingerprint(),
                l.getService(),
                l.getType()
            ))
            .containsExactly(tuple(
                savedLetter.getId(),
                savedLetter.getChecksum(),
                savedLetter.getCreatedAt(),
                savedLetter.getEncryptionKeyFingerprint(),
                savedLetter.getService(),
                savedLetter.getType()
            ));
    }

    private void storeLetter(LetterStatus status, String type) {
        Letter letter = SampleData.letterEntity("service1", LocalDateTime.now(), type);
        letter.setStatus(status);
        repository.save(letter);
    }
}
