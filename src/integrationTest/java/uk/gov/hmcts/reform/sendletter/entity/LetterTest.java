package uk.gov.hmcts.reform.sendletter.entity;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class LetterTest {

    @Autowired
    private LetterRepository repository;

    @Test
    void should_successfully_save_report_in_db() {
        repository.save(SampleData.letterEntity("a.service"));
        List<Letter> letters = Lists.newArrayList(repository.findAll());
        assertThat(letters.size()).isEqualTo(1);
        assertThat(letters.get(0).getStatus()).isEqualTo(LetterStatus.Created);
    }

    @Test
    void finds_letters_by_id_and_service() {
        repository.save(SampleData.letterEntity("a.service"));
        Letter second = SampleData.letterEntity("different");
        repository.save(second);

        Letter found = repository.findByIdAndService(second.getId(), second.getService()).get();
        assertThat(found.getId()).isEqualTo(second.getId());
    }
}
