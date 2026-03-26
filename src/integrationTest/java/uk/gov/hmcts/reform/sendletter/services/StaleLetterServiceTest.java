package uk.gov.hmcts.reform.sendletter.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterEventRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class StaleLetterServiceTest {

    private StaleLetterService service;

    @Autowired
    private LetterRepository letterRepository;

    @Autowired
    private LetterEventRepository letterEventRepository;

    @BeforeEach
    void setUp() {
        service = new StaleLetterService(
            null,
            letterRepository,
            letterEventRepository,
            2,
            "23:59",
            null
        );
    }

    @AfterEach
    void tearDown() {
        letterRepository.deleteAll();
    }

    @Test
    void getStaleLettersWithValidPrintDateShouldRetrieveCorrectLetters() {
        Letter letterA = SampleData.letterEntity("some_service_name", LocalDateTime.now(ZoneOffset.UTC).minusDays(10));
        letterA.setSentToPrintAt(null);
        letterA.setStatus(LetterStatus.Uploaded);
        letterRepository.save(letterA);

        Letter letterB = SampleData.letterEntity("some_service_name", LocalDateTime.now(ZoneOffset.UTC).minusDays(10));
        letterB.setSentToPrintAt(LocalDateTime.now().minusDays(10));
        letterB.setStatus(LetterStatus.Uploaded);
        letterRepository.save(letterB);

        List<BasicLetterInfo> letters = service.getStaleLettersWithValidPrintDate(
            List.of(LetterStatus.Uploaded),
            LocalDateTime.now(ZoneOffset.UTC).minusDays(7)
        );

        assertThat(letters).hasSize(1);
        assertThat(letters.get(0).getSentToPrintAt()).isNotNull();

    }
}
