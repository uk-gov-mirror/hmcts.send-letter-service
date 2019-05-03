package uk.gov.hmcts.reform.sendletter.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public final class CsvWriter {

    private static final String[] LETTERS_COUNT_SUMMARY_CSV_HEADERS = {
        "Service", "Letters Uploaded"
    };

    private CsvWriter() {
        // utility class constructor
    }

    public static File writeLettersCountSummaryToCsv(
        List<LettersCountSummary> lettersCountSummary
    ) throws IOException {
        File csvFile = File.createTempFile("Letters-count-summary-", ".csv");

        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(LETTERS_COUNT_SUMMARY_CSV_HEADERS);
        FileWriter fileWriter = new FileWriter(csvFile);

        try (CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)) {
            for (LettersCountSummary summary : lettersCountSummary) {
                printer.printRecord(summary.service, summary.uploaded);
            }
        }
        return csvFile;
    }
}
