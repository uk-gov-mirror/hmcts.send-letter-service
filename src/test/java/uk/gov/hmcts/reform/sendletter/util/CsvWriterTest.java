package uk.gov.hmcts.reform.sendletter.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    private List<CSVRecord> readCsv(File file) throws IOException {
        return CSVFormat.DEFAULT.parse(new FileReader(file)).getRecords();
    }
}
