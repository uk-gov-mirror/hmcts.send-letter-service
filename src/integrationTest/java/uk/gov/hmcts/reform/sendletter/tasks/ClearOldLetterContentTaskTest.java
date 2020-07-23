package uk.gov.hmcts.reform.sendletter.tasks;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.LetterDataAccessService;

import java.time.Clock;
import java.time.Duration;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class ClearOldLetterContentTaskTest {

    @Autowired LetterRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    void should_clear_content_of_old_letters() {
        // given
        Letter letter = SampleData.letterEntity("bulkprint");
        letter.setStatus(LetterStatus.Uploaded);
        letter.setFileContent("file-content".getBytes());

        repository.saveAndFlush(letter);

        var task = new ClearOldLetterContentTask(
            new LetterDataAccessService(repository),
            Duration.ofSeconds(0), // clear immediately
            Clock.systemDefaultZone()
        );

        // when
        task.run();

        // then
        entityManager.flush();
        var letterAfterTaskRun = repository.findById(letter.getId());
        assertThat(letterAfterTaskRun)
            .hasValueSatisfying(
                new Condition<>(
                    l -> l.getFileContent() == null,
                    "File content should be cleared"
                ));
    }
}
