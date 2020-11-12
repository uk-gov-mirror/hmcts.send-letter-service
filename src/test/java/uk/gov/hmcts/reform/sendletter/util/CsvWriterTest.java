package uk.gov.hmcts.reform.sendletter.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

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
        System.out.println(localDateTimes[0]);
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

    private List<CSVRecord> readCsv(File file) throws IOException {
        return CSVFormat.DEFAULT.parse(new FileReader(file)).getRecords();
    }
}
