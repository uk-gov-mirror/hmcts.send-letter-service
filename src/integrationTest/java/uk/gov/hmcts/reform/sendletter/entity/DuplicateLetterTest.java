package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class DuplicateLetterTest {
    @Autowired
    DuplicateRepository duplicateRepository;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Integer> document = Map.of("Document_1", 1);

    @BeforeEach
    void setUp() {
        duplicateRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        duplicateRepository.deleteAll();
    }

    @Test
    void should_Save_Duplicate() throws JsonProcessingException {
        UUID uuid = UUID.randomUUID();
        DuplicateLetter duplicateLetter = new DuplicateLetter(uuid,
                "checksum",
                "a.service",
                objectMapper.readTree("{}"),
                "test",
                LocalDateTime.now(),
                objectMapper.valueToTree(document),
                "true"
        );
        duplicateRepository.save(duplicateLetter);
        Optional<DuplicateLetter> optDuplicateLetter = duplicateRepository.findById(uuid);
        assertThat(optDuplicateLetter.isPresent()).isEqualTo(true);
        DuplicateLetter savedDuplicateLetter = optDuplicateLetter.get();
        assertThat(savedDuplicateLetter.getId()).isEqualTo(uuid);
        assertThat(savedDuplicateLetter.getChecksum()).isEqualTo("checksum");
        assertThat(savedDuplicateLetter.getService()).isEqualTo("a.service");
        assertThat(savedDuplicateLetter.getAdditionalData()).isInstanceOf(JsonNode.class);
        assertThat(savedDuplicateLetter.getType()).isEqualTo("test");
        assertThat(savedDuplicateLetter.getCreatedAt()).isInstanceOf(LocalDateTime.class);
        assertThat(objectMapper.convertValue(savedDuplicateLetter.getCopies(),
                new TypeReference<Map<String, Integer>>() {})).containsAllEntriesOf(document);
        assertThat(savedDuplicateLetter.getIsAsync()).isEqualTo("true");
    }


    @Test
    void should_Save_Duplicate_Default_Contructor() throws JsonProcessingException {
        UUID uuid = UUID.randomUUID();
        DuplicateLetter duplicateLetter = new DuplicateLetter();
        duplicateLetter.setId(uuid);
        duplicateLetter.setChecksum("checksum");
        duplicateLetter.setService("a.service");
        duplicateLetter.setAdditionalData(new ObjectMapper().readTree("{}"));
        duplicateLetter.setType("test");
        duplicateLetter.setCreatedAt(LocalDateTime.now());
        duplicateLetter.setIsAsync("true");
        duplicateLetter.setCopies(objectMapper.valueToTree(document));

        duplicateRepository.save(duplicateLetter);
        Optional<DuplicateLetter> optDuplicateLetter = duplicateRepository.findById(uuid);
        assertThat(optDuplicateLetter.isPresent()).isEqualTo(true);
        DuplicateLetter savedDuplicateLetter = optDuplicateLetter.get();
        assertThat(savedDuplicateLetter.getId()).isEqualTo(uuid);
        assertThat(savedDuplicateLetter.getChecksum()).isEqualTo("checksum");
        assertThat(savedDuplicateLetter.getService()).isEqualTo("a.service");
        assertThat(savedDuplicateLetter.getAdditionalData()).isInstanceOf(JsonNode.class);
        assertThat(savedDuplicateLetter.getType()).isEqualTo("test");
        assertThat(savedDuplicateLetter.getCreatedAt()).isInstanceOf(LocalDateTime.class);
        assertThat(objectMapper.convertValue(savedDuplicateLetter.getCopies(),
                new TypeReference<Map<String, Integer>>() {})).containsAllEntriesOf(document);
        assertThat(savedDuplicateLetter.getIsAsync()).isEqualTo("true");
    }
}
