package uk.gov.hmcts.reform.sendletter.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DelayedPrintServiceTest {
    @Mock
    private LetterRepository letterRepository;

    private DelayedPrintService delayedPrintService;

    @BeforeEach
    void setUp() {
        delayedPrintService = new DelayedPrintService(letterRepository);
    }

    @Test
    void should_return_delayed_print_file() throws IOException {
        LocalDateTime current = LocalDateTime.now();
        LocalDateTime createdAt = current.minusDays(6);
        List<BasicLetterInfo> letters = Arrays.asList(createLetter(createdAt, 3),
            createLetter(createdAt, 2), createLetter(createdAt, 5),
            createLetter(createdAt, 1));
        Stream<BasicLetterInfo> stream = letters.stream();
        //given
        given(letterRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(eq(LetterStatus.Posted),
            isA(LocalDateTime.class), isA(LocalDateTime.class))).willReturn(stream);

        File deplayLettersAttachment = delayedPrintService.getDelayLettersAttachment(
                current.minusDays(6), current, 2);
        List<CSVRecord> csvRecords = readCsv(deplayLettersAttachment);
        assertThat(csvRecords.size()).isEqualTo(3); // Includes header
        verify(letterRepository).findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(eq(LetterStatus.Posted),
            isA(LocalDateTime.class), isA(LocalDateTime.class));
    }

    private BasicLetterInfo createLetter(LocalDateTime createdAt, int daysToPrint) {
        return new BasicLetterInfo(UUID.randomUUID(), "checksum", "testService",
            LetterStatus.Posted, "type-1", "encryptionKeyFingerprint",
            createdAt, createdAt.plusMinutes(10), createdAt.plusDays(daysToPrint));
    }

    private List<CSVRecord> readCsv(File file) throws IOException {
        return CSVFormat.DEFAULT.parse(new FileReader(file)).getRecords();
    }


}
