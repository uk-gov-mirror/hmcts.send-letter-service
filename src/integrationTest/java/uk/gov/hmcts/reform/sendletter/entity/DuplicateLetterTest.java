package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class DuplicateLetterTest {
    @Autowired
    DuplicateRepository duplicateRepository;

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
        DuplicateLetter duplicateLetter = new DuplicateLetter(
                uuid,
                "checksum",
                "a.service",
                new ObjectMapper().readTree("{}"),
                "test",
               "true".getBytes(),
                true,
                "EncryptionKeyFingerprint",
                LocalDateTime.now(),
                0,
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
        assertThat(savedDuplicateLetter.getFileContent()).isEqualTo("true".getBytes());
        assertThat(savedDuplicateLetter.getType()).isEqualTo("test");
        assertThat(savedDuplicateLetter.getEncryptionKeyFingerprint()).isEqualTo("EncryptionKeyFingerprint");
        assertThat(savedDuplicateLetter.getCreatedAt()).isInstanceOf(LocalDateTime.class);
        assertThat(savedDuplicateLetter.getCopies()).isEqualTo(0);
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
        duplicateLetter.setFileContent("true".getBytes());
        duplicateLetter.setEncrypted(true);
        duplicateLetter.setEncryptionKeyFingerprint("EncryptionKeyFingerprint");
        duplicateLetter.setCreatedAt(LocalDateTime.now());
        duplicateLetter.setCopies(0);
        duplicateLetter.setIsAsync("true");

        duplicateRepository.save(duplicateLetter);
        Optional<DuplicateLetter> optDuplicateLetter = duplicateRepository.findById(uuid);
        assertThat(optDuplicateLetter.isPresent()).isEqualTo(true);
        DuplicateLetter savedDuplicateLetter = optDuplicateLetter.get();
        assertThat(savedDuplicateLetter.getId()).isEqualTo(uuid);
        assertThat(savedDuplicateLetter.getChecksum()).isEqualTo("checksum");
        assertThat(savedDuplicateLetter.getService()).isEqualTo("a.service");
        assertThat(savedDuplicateLetter.getAdditionalData()).isInstanceOf(JsonNode.class);
        assertThat(savedDuplicateLetter.getFileContent()).isEqualTo("true".getBytes());
        assertThat(savedDuplicateLetter.getType()).isEqualTo("test");
        assertThat(savedDuplicateLetter.getEncryptionKeyFingerprint()).isEqualTo("EncryptionKeyFingerprint");
        assertThat(savedDuplicateLetter.getCreatedAt()).isInstanceOf(LocalDateTime.class);
        assertThat(savedDuplicateLetter.getCopies()).isEqualTo(0);
        assertThat(savedDuplicateLetter.getIsAsync()).isEqualTo("true");
    }
}
