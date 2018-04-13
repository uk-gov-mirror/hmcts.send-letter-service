package uk.gov.hmcts.reform.sendletter.entity;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class LetterTest {

    @Autowired
    private LetterRepository repository;

    @Test
    public void should_successfully_save_report_in_db() {
        repository.save(SampleData.letterEntity("a.service"));
        List<Letter> letters = Lists.newArrayList(repository.findAll());
        assertThat(letters.size()).isEqualTo(1);
        assertThat(letters.get(0).getStatus()).isEqualTo(LetterStatus.Created);
    }

    @Test
    public void finds_letters_by_id_and_service() {
        repository.save(SampleData.letterEntity("a.service"));
        Letter second = SampleData.letterEntity("different");
        repository.save(second);

        Letter found = repository.findByIdAndService(second.getId(), second.getService()).get();
        assertThat(found.getId()).isEqualTo(second.getId());
    }
}
