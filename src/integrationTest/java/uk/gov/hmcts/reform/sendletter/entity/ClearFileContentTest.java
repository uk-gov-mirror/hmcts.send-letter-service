package uk.gov.hmcts.reform.sendletter.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Uploaded;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class ClearFileContentTest {

    @Autowired
    LetterRepository repo;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repo.deleteAll();
    }

    @Test
    void should_clear_file_content_in_letters_created_before_specified_date_and_in_given_status() {
        // given
        var cutOffDate = LocalDateTime.now().minusDays(2);

        Letter l1 = storeLetter(cutOffDate.minusSeconds(10), Created); // age ✔️, status ✖️
        Letter l2 = storeLetter(cutOffDate.minusSeconds(20), Uploaded); // age ✔️, status ✔️
        Letter l3 = storeLetter(cutOffDate.plusSeconds(30), Uploaded); // age ✖️, status ✔️

        // when
        int updateCount = repo.clearFileContent(cutOffDate, Uploaded);

        // then
        assertThat(updateCount).isEqualTo(1);
        assertThat(repo.findAll())
            .extracting(l ->
                tuple(l.getId(), l.getFileContent())
            )
            .containsExactlyInAnyOrder(
                tuple(l1.getId(), l1.getFileContent()),
                tuple(l2.getId(), null),
                tuple(l3.getId(), l3.getFileContent())
            );
    }


    Letter storeLetter(LocalDateTime createdAt, LetterStatus status) {
        Letter letter = SampleData.letterEntity("service1", createdAt, "type1");
        letter.setStatus(status);
        letter.setFileContent("blah blah".getBytes());
        return repo.save(letter);
    }
}
