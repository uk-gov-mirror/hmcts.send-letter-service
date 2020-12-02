package uk.gov.hmcts.reform.sendletter.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.services.util.FinalPackageFileNameHelper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvWriterTest {

    @Test
    void should_return_csv_file_with_headers_and_csv_records() throws IOException {
        //given
        List<LettersCountSummary> data = Arrays.asList(
            new LettersCountSummary("ServiceA", 10),
            new LettersCountSummary("ServiceB", 20),
            new LettersCountSummary("ServiceC", 30)
        );

        //when
        File csvFile = CsvWriter.writeLettersCountSummaryToCsv(data);

        //then
        List<CSVRecord> csvRecordsList = readCsv(csvFile);

        assertThat(csvRecordsList)
            .isNotEmpty()
            .hasSize(4)
            .extracting(record -> tuple(
                record.get(0), record.get(1))
            ).containsExactly(
            tuple("Service", "Letters Uploaded"),
            tuple("ServiceA", "10"),
            tuple("ServiceB", "20"),
            tuple("ServiceC", "30"));
    }

    @Test
    void should_return_csv_file_with_only_headers_when_the_data_is_empty() throws IOException {
        //when
        File csvFile = CsvWriter.writeLettersCountSummaryToCsv(emptyList());

        //then
        List<CSVRecord> csvRecordsList = readCsv(csvFile);

        assertThat(csvRecordsList)
            .isNotEmpty()
            .hasSize(1)
            .extracting(record -> tuple(
                record.get(0), record.get(1))
            ).containsExactly(
            tuple("Service", "Letters Uploaded"));

    }

    @Test
    void should_return_stale_letters() throws IOException {
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        LocalDateTime[] localDateTimes = {LocalDateTime.now(), LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2)};

        //given
        List<BasicLetterInfo> staleLetters = Arrays.asList(
                new BasicLetterInfo(uuids[0], null, "Test", LetterStatus.Uploaded,
                        null, null, localDateTimes[0], localDateTimes[0], null),
                new BasicLetterInfo(uuids[1], null, "Test", LetterStatus.Uploaded,
                        null, null, localDateTimes[1], localDateTimes[1], null),
                new BasicLetterInfo(uuids[2], null, "Test", LetterStatus.Uploaded,
                        null, null, localDateTimes[2], localDateTimes[2], null)
        );

        //when
        File csvFile =  CsvWriter.writeStaleLettersToCsv(staleLetters);
        //then
        List<CSVRecord> csvRecordsList = readCsv(csvFile);
        assertThat(csvRecordsList)
                .isNotEmpty()
                .hasSize(4)
                .extracting(record -> tuple(
                        record.get(0), record.get(1), record.get(2), record.get(3), record.get(4))
                ).containsExactly(
                tuple("Id", "Status", "Service", "CreatedAt", "SentToPrintAt"),
                tuple(uuids[0].toString(), LetterStatus.Uploaded.name(), "Test",
                        localDateTimes[0].toString(), localDateTimes[0].toString()),
                tuple(uuids[1].toString(), LetterStatus.Uploaded.name(), "Test",
                        localDateTimes[1].toString(), localDateTimes[1].toString()),
                tuple(uuids[2].toString(), LetterStatus.Uploaded.name(), "Test",
                        localDateTimes[2].toString(), localDateTimes[2].toString()));

    }

    @Test
    void should_return_stale_letters_with_only_headers_when_the_data_is_empty() throws IOException {
        //when
        File csvFile = CsvWriter.writeStaleLettersToCsv(emptyList());

        //then
        List<CSVRecord> csvRecordsList = readCsv(csvFile);

        assertThat(csvRecordsList)
                .isNotEmpty()
                .hasSize(1)
                .extracting(record -> tuple(
                        record.get(0), record.get(1), record.get(2), record.get(3), record.get(4))
                ).containsExactly(
                tuple("Id", "Status", "Service", "CreatedAt", "SentToPrintAt"));

    }

    @Test
    void should_return_delayed_posted_letters() throws IOException {
        List<Letter> letters = Arrays.asList(createLetter(), createLetter(), createLetter());
        Stream<Letter> stream = letters.stream();
        File file = CsvWriter.writeDelayedPostedLettersToCsv(stream);
        List<CSVRecord> csvRecords = readCsv(file);

        assertThat(csvRecords).isNotEmpty().hasSize(4)
                .extracting(record -> tuple(
                        record.get(0), record.get(1), record.get(2), record.get(3), record.get(4))
                ).containsExactly(
                tuple("FileName", "ServiceName", "ReceivedDate", "UploadedDate", "PrintedDate"),
                tuple(generateName(letters.get(0).getType(), letters.get(0).getService(),
                        letters.get(0).getCreatedAt(), letters.get(0).getId(), letters.get(0).isEncrypted()),
                        letters.get(0).getService(),
                        letters.get(0).getCreatedAt().toString(),
                        letters.get(0).getSentToPrintAt().toString(),
                        letters.get(0).getPrintedAt().toString()),
                tuple(generateName(letters.get(1).getType(), letters.get(1).getService(),
                        letters.get(1).getCreatedAt(), letters.get(1).getId(), letters.get(1).isEncrypted()),
                        letters.get(1).getService(),
                        letters.get(1).getCreatedAt().toString(),
                        letters.get(1).getSentToPrintAt().toString(),
                        letters.get(1).getPrintedAt().toString()),
                tuple(generateName(letters.get(2).getType(), letters.get(2).getService(),
                        letters.get(2).getCreatedAt(), letters.get(2).getId(), letters.get(2).isEncrypted()),
                        letters.get(2).getService(),
                        letters.get(2).getCreatedAt().toString(),
                        letters.get(2).getSentToPrintAt().toString(),
                        letters.get(2).getPrintedAt().toString()));

    }

    @Test
    void should_return_stale_posted_letters() throws IOException {
        List<Letter> letters = Arrays.asList(createLetter(), createLetter(), createLetter());
        Stream<Letter> stream = letters.stream();
        File file = CsvWriter.writeStaleLettersReport(stream);
        List<CSVRecord> csvRecords = readCsv(file);

        assertThat(csvRecords).isNotEmpty().hasSize(4)
                .extracting(record -> tuple(
                        record.get(0), record.get(1), record.get(2), record.get(3))
                ).contains(
                tuple("FileName", "ServiceName", "ReceivedDate", "UploadedDate"),
                tuple(generateName(letters.get(0).getType(), letters.get(0).getService(),
                        letters.get(0).getCreatedAt(), letters.get(0).getId(), letters.get(0).isEncrypted()),
                        letters.get(0).getService(),
                        letters.get(0).getCreatedAt().toString(),
                        letters.get(0).getSentToPrintAt().toString()),
                tuple(generateName(letters.get(1).getType(), letters.get(1).getService(),
                        letters.get(1).getCreatedAt(), letters.get(1).getId(), letters.get(1).isEncrypted()),
                        letters.get(1).getService(),
                        letters.get(1).getCreatedAt().toString(),
                        letters.get(1).getSentToPrintAt().toString()),
                tuple(generateName(letters.get(2).getType(), letters.get(2).getService(),
                        letters.get(2).getCreatedAt(), letters.get(2).getId(), letters.get(2).isEncrypted()),
                        letters.get(2).getService(),
                        letters.get(2).getCreatedAt().toString(),
                        letters.get(2).getSentToPrintAt().toString()));

    }

    @Test
    void should_return_only_two_delayed_posted_letters() throws IOException {
        List<Letter> letters = Arrays.asList(createLetter(), createExceptionLetter(), createLetter());
        Stream<Letter> stream = letters.stream();
        File file = CsvWriter.writeDelayedPostedLettersToCsv(stream);
        List<CSVRecord> csvRecords = readCsv(file);

        assertThat(csvRecords).isNotEmpty().hasSize(3)
                .extracting(record -> tuple(
                        record.get(0), record.get(1), record.get(2), record.get(3), record.get(4))
                ).containsExactly(
                tuple("FileName", "ServiceName", "ReceivedDate", "UploadedDate", "PrintedDate"),
                tuple(generateName(letters.get(0).getType(), letters.get(0).getService(),
                        letters.get(0).getCreatedAt(), letters.get(0).getId(), letters.get(0).isEncrypted()),
                        letters.get(0).getService(),
                        letters.get(0).getCreatedAt().toString(),
                        letters.get(0).getSentToPrintAt().toString(),
                        letters.get(0).getPrintedAt().toString()),
                tuple(generateName(letters.get(2).getType(), letters.get(2).getService(),
                        letters.get(2).getCreatedAt(), letters.get(2).getId(), letters.get(2).isEncrypted()),
                        letters.get(2).getService(),
                        letters.get(2).getCreatedAt().toString(),
                        letters.get(2).getSentToPrintAt().toString(),
                        letters.get(2).getPrintedAt().toString()));

    }

    @Test
    void should_return_only_two_stale_letters() throws IOException {
        List<Letter> letters = Arrays.asList(createLetter(), createExceptionLetter(), createLetter());
        Stream<Letter> stream = letters.stream();
        File file = CsvWriter.writeStaleLettersReport(stream);
        List<CSVRecord> csvRecords = readCsv(file);

        assertThat(csvRecords).isNotEmpty().hasSize(3)
                .extracting(record -> tuple(
                        record.get(0), record.get(1), record.get(2), record.get(3))
                ).contains(
                tuple("FileName", "ServiceName", "ReceivedDate", "UploadedDate"),
                tuple(generateName(letters.get(0).getType(), letters.get(0).getService(),
                        letters.get(0).getCreatedAt(), letters.get(0).getId(), letters.get(0).isEncrypted()),
                        letters.get(0).getService(),
                        letters.get(0).getCreatedAt().toString(),
                        letters.get(0).getSentToPrintAt().toString()),
                tuple(generateName(letters.get(2).getType(), letters.get(2).getService(),
                        letters.get(2).getCreatedAt(), letters.get(2).getId(), letters.get(2).isEncrypted()),
                        letters.get(2).getService(),
                        letters.get(2).getCreatedAt().toString(),
                        letters.get(2).getSentToPrintAt().toString()));

    }

    private List<CSVRecord> readCsv(File file) throws IOException {
        return CSVFormat.DEFAULT.parse(new FileReader(file)).getRecords();
    }

    public static String generateName(
            String type,
            String service,
            LocalDateTime createdAtDateTime,
            UUID id,
            Boolean isEncrypted
    ) {
        return String.format(
                "%s_%s_%s_%s.%s",
                type.replace("_", ""),
                service.replace("_", ""),
                createdAtDateTime.format(FinalPackageFileNameHelper.dateTimeFormatter),
                id,
                isEncrypted ? "pgp" : "zip"
        );
    }

    private Letter createLetter() {
        LocalDateTime current = LocalDateTime.now();
        Letter result = mock(Letter.class);
        when(result.getType()).thenReturn("type-1");
        when(result.getService()).thenReturn("testService");
        when(result.getCreatedAt()).thenReturn(current);
        when(result.getId()).thenReturn(UUID.randomUUID());
        when(result.isEncrypted()).thenReturn(true);
        when(result.getSentToPrintAt()).thenReturn(current.plusMinutes(10));
        when(result.getPrintedAt()).thenReturn(current.plusDays(3));
        return result;
    }

    private Letter createExceptionLetter() {
        LocalDateTime current = LocalDateTime.now();
        Letter result = mock(Letter.class);
        when(result.getCreatedAt()).thenReturn(current);
        when(result.getType()).thenThrow(new RuntimeException("Exception occured"));
        return result;
    }
}
