package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class LetterTest {

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
    void should_successfully_save_report_in_db() throws JsonProcessingException {
        repository.save(SampleData.letterEntity("a.service"));
        List<Letter> letters = Lists.newArrayList(repository.findAll());
        assertThat(letters.size()).isEqualTo(1);
        assertThat(letters.get(0).getStatus()).isEqualTo(LetterStatus.Created);
        JsonNode expectedAdditionalData = new ObjectMapper().readTree("{\"Document_1\":1}");
        assertThat(letters.get(0).getCopies()).isEqualTo(expectedAdditionalData);
    }

    @Test
    void should_pick_one_recently_saved_in_db() {
        repository.save(SampleData.letterEntity("a.service"));
        repository.save(SampleData.letterEntity("a.service"));
        List<Letter> letters = Lists.newArrayList(repository.findAll());
        assertThat(letters.size()).isEqualTo(2);
        Optional<Letter> findLetter = repository.findFirstLetterCreated(LocalDateTime.now().minusMinutes(0));
        assertThat(findLetter.isPresent()).isEqualTo(true);
        assertThat(letters.get(0).getChecksum()).isEqualTo(findLetter.get().getChecksum());
    }

    @Test
    void should_not_return_result_saved_in_db() {
        repository.save(SampleData.letterEntity("a.service"));
        repository.save(SampleData.letterEntity("a.service"));
        List<Letter> letters = Lists.newArrayList(repository.findAll());
        assertThat(letters.size()).isEqualTo(2);
        Optional<Letter> findLetter = repository.findFirstLetterCreated(LocalDateTime.now().minusMinutes(3));
        assertThat(findLetter.isPresent()).isEqualTo(false);
    }
}
