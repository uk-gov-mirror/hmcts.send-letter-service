package uk.gov.hmcts.reform.sendletter.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.services.util.FinalPackageFileNameHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class CsvWriter {
    private static final Logger logger = LoggerFactory.getLogger(CsvWriter.class);

    private static final String[] LETTERS_COUNT_SUMMARY_CSV_HEADERS = {
        "Service", "Letters Uploaded"
    };

    private static final String[] STALE_LETTERS_CSV_HEADERS = {
        "Id", "Status", "Service", "CreatedAt", "SentToPrintAt"
    };

    private static final String[] DELAYED_LETTERS_EMAIL_CSV_HEADERS = {
        "FileName", "ServiceName", "ReceivedDate", "UploadedDate", "PrintedDate"
    };

    private static final String[] STALE_LETTERS_EMAIL_CSV_HEADERS = {
        "FileName", "ServiceName", "ReceivedDate", "UploadedDate"
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
                printer.printRecord(summary.serviceName, summary.uploaded);
            }
        }
        return csvFile;
    }

    public static File writeStaleLettersToCsv(List<BasicLetterInfo> staleLetters) throws IOException {
        File csvFile = File.createTempFile("Stale-letters-", ".csv");
        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(STALE_LETTERS_CSV_HEADERS);
        FileWriter fileWriter = new FileWriter(csvFile);

        try (CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)) {
            for (BasicLetterInfo staleLetter : staleLetters) {
                printer.printRecord(staleLetter.getId(), staleLetter.getStatus(),
                        staleLetter.getService(), staleLetter.getCreatedAt(), staleLetter.getSentToPrintAt());
            }
        }

        return csvFile;
    }

    public static File writeDelayedPostedLettersToCsv(Stream<BasicLetterInfo> letters) throws IOException {
        File csvFIle = File.createTempFile("Deplayed-letters-", ".csv");
        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(DELAYED_LETTERS_EMAIL_CSV_HEADERS);
        FileWriter fileWriter = new FileWriter(csvFIle);
        AtomicInteger count = new AtomicInteger(0);

        try (CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)) {
            letters.forEach(letter -> printDelayRecords(letter, printer, count));
        }

        logger.info("Number of weekly delayed print letters {}", count.get());
        return csvFIle;
    }

    public static File writeStaleLettersReport(Stream<BasicLetterInfo> letters) throws IOException {
        File csvFIle = File.createTempFile("Stale-letters-", ".csv");
        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(STALE_LETTERS_EMAIL_CSV_HEADERS);
        FileWriter fileWriter = new FileWriter(csvFIle);
        AtomicInteger count = new AtomicInteger(0);

        try (CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)) {
            letters.forEach(letter -> printStaleRecords(letter, printer, count));
        }

        logger.info("Number of weekly stale letters {}", count.get());
        return csvFIle;
    }

    private static void printStaleRecords(BasicLetterInfo letter, CSVPrinter printer, AtomicInteger count) {
        try {
            printer.printRecord(FinalPackageFileNameHelper.generateName(letter.getType(),
                letter.getService(), letter.getCreatedAt(), letter.getId(), true),
                    letter.getService(), letter.getCreatedAt(),
                    letter.getSentToPrintAt());
            count.incrementAndGet();
        } catch (Exception e) {
            logger.error("Stale letter exception ", e);
        }
    }

    private static void printDelayRecords(BasicLetterInfo letter, CSVPrinter printer, AtomicInteger count) {
        try {
            printer.printRecord(FinalPackageFileNameHelper.generateName(letter.getType(),
                letter.getService(), letter.getCreatedAt(), letter.getId(), true),
                    letter.getService(), letter.getCreatedAt(),
                    letter.getSentToPrintAt(), letter.getPrintedAt());
            count.incrementAndGet();
        } catch (Exception e) {
            logger.error("Deplay letter posted exception ", e);
        }
    }
}
