package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    @Test
    public void should_save_checksum_to_two_columns() throws Exception {
        // given
        Letter letter = new Letter(
            UUID.randomUUID(),
            "messageId_aka_checksum",
            "service",
            new ObjectMapper().readTree("{}"),
            "a type",
            new byte[1],
            false,
            Timestamp.valueOf(LocalDateTime.now())
        );

        // when
        repository.save(letter);
        Letter inDb = repository.findById(letter.getId()).get();

        // then
        assertThat(inDb.getMessageId()).isEqualTo("messageId_aka_checksum");
        assertThat(inDb.getChecksum()).isEqualTo("messageId_aka_checksum");
    }
}
