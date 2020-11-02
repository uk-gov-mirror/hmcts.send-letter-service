package uk.gov.hmcts.reform.sendletter.entity;

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
public class ExceptionLetterTest {
    @Autowired
    ExceptionRepository exceptionRepository;

    @BeforeEach
    void setUp() {
        exceptionRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        exceptionRepository.deleteAll();
    }

    @Test
    void should_Save_Exception() {
        UUID uuid = UUID.randomUUID();
        ExceptionLetter exceptionLetter = new ExceptionLetter(
                uuid,
                "a.service",
                LocalDateTime.now(),
                "test",
                "Error",
                "true"
        );

        exceptionRepository.save(exceptionLetter);
        Optional<ExceptionLetter> optExceptionLetter = exceptionRepository.findById(uuid);
        assertThat(optExceptionLetter.isPresent()).isEqualTo(true);
        ExceptionLetter savedExceptionLetter = optExceptionLetter.get();
        assertThat(savedExceptionLetter.getId()).isEqualTo(uuid);
        assertThat(savedExceptionLetter.getService()).isEqualTo("a.service");
        assertThat(savedExceptionLetter.getType()).isEqualTo("test");
        assertThat(savedExceptionLetter.getCreatedAt()).isInstanceOf(LocalDateTime.class);
        assertThat(savedExceptionLetter.getIsAsync()).isEqualTo("true");
        assertThat(savedExceptionLetter.getMessage()).isEqualTo("Error");
    }

    @Test
    void should_Save_default_constructor_Exception() {
        UUID uuid = UUID.randomUUID();
        ExceptionLetter exceptionLetter = new ExceptionLetter();
        exceptionLetter.setId(uuid);
        exceptionLetter.setService("a.service");
        exceptionLetter.setCreatedAt(LocalDateTime.now());
        exceptionLetter.setType("test");
        exceptionLetter.setMessage("Error");
        exceptionLetter.setIsAsync("true");

        exceptionRepository.save(exceptionLetter);
        Optional<ExceptionLetter> optExceptionLetter = exceptionRepository.findById(uuid);
        assertThat(optExceptionLetter.isPresent()).isEqualTo(true);
        ExceptionLetter savedExceptionLetter = optExceptionLetter.get();
        assertThat(savedExceptionLetter.getId()).isEqualTo(uuid);
        assertThat(savedExceptionLetter.getService()).isEqualTo("a.service");
        assertThat(savedExceptionLetter.getType()).isEqualTo("test");
        assertThat(savedExceptionLetter.getCreatedAt()).isInstanceOf(LocalDateTime.class);
        assertThat(savedExceptionLetter.getIsAsync()).isEqualTo("true");
        assertThat(savedExceptionLetter.getMessage()).isEqualTo("Error");
    }
}
