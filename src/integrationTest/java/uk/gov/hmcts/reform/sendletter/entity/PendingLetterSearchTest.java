package uk.gov.hmcts.reform.sendletter.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Uploaded;
import static uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask.SMOKE_TEST_LETTER_TYPE;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class PendingLetterSearchTest {

    private static final String SMOKE_TEST_LETTER_TYPE = "smoke_test";

    @Autowired
    private LetterRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void should_return_letters_in_created_status() {
        // given
        storeLetter(Uploaded, "type-1", LocalDateTime.now());
        storeLetter(Uploaded, "type-2", LocalDateTime.now());
        storeLetter(Created, "type-3", LocalDateTime.now());
        storeLetter(Created, "type-4", LocalDateTime.now());

        // when
        List<BasicLetterInfo> letters = repository.findPendingLetters();

        // then
        assertThat(letters)
            .extracting(BasicLetterInfo::getType)
            .containsExactly("type-3", "type-4");
    }

    @Test
    public void should_return_letters_in_created_status_before_given_time() {
        LocalDateTime currentTime = LocalDateTime.now();
        // given
        storeLetter(Uploaded, "type-1", currentTime.minusMinutes(10));
        storeLetter(Uploaded, "type-2", currentTime.minusMinutes(10));
        storeLetter(Created, "type-3", currentTime.minusMinutes(10));
        storeLetter(Created, "type-4", currentTime.minusMinutes(10));
        storeLetter(Created, "type-5", currentTime.minusMinutes(2));

        // when
        List<BasicLetterInfo> letters = repository
                .findByCreatedAtBeforeAndStatusAndTypeNot(currentTime.minusMinutes(5), Created,
                        UploadLettersTask.SMOKE_TEST_LETTER_TYPE);

        // then
        assertThat(letters)
            .extracting(BasicLetterInfo::getType)
            .containsOnly("type-3", "type-4");
    }

    @Test
    public void should_not_include_smoke_test_letters_in_the_result() {
        // given
        storeLetter(Created, SMOKE_TEST_LETTER_TYPE, LocalDateTime.now());
        storeLetter(Created, "not-smoke-test-type-1", LocalDateTime.now());
        storeLetter(Created, "not-smoke-test-type-2", LocalDateTime.now());

        // when
        List<BasicLetterInfo> letters = repository.findPendingLetters();

        // then
        assertThat(letters.size()).isEqualTo(2);
        assertThat(letters).noneMatch(l -> l.getStatus().equals(SMOKE_TEST_LETTER_TYPE));
    }

    @Test
    public void should_not_include_smoke_test_letters_in_the_result_before_given_time() {
        LocalDateTime currentTime = LocalDateTime.now();
        // given
        storeLetter(Created, SMOKE_TEST_LETTER_TYPE, currentTime.minusMinutes(30));
        storeLetter(Created, "not-smoke-test-type-1", currentTime.minusMinutes(30));
        storeLetter(Created, "not-smoke-test-type-2", currentTime.minusMinutes(30));

        // when
        List<BasicLetterInfo> letters = repository
                .findByCreatedAtBeforeAndStatusAndTypeNot(currentTime.minusMinutes(5), Created,
                        UploadLettersTask.SMOKE_TEST_LETTER_TYPE);

        // then
        assertThat(letters.size()).isEqualTo(2);
        assertThat(letters).noneMatch(l -> l.getStatus().equals(SMOKE_TEST_LETTER_TYPE));
    }

    @Test
    public void should_set_properties_correctly() {
        // given
        Letter letter = SampleData.letterEntity("service", LocalDateTime.now(), "type", "fingerprint", 1,
                SampleData.checkSumSupplier);
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



    private void storeLetter(LetterStatus status, String type, LocalDateTime createdAt) {
        Letter letter = SampleData.letterEntity("service1", createdAt, type);
        letter.setStatus(status);
        repository.save(letter);
    }
}
