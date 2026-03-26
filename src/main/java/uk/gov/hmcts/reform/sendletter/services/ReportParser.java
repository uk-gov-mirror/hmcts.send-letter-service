package uk.gov.hmcts.reform.sendletter.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exception.ReportParsingException;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintStatus;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;
import uk.gov.hmcts.reform.sendletter.model.Report;
import uk.gov.hmcts.reform.sendletter.services.util.FileNameHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * Utility class to parse reports.
 */
@Component
public class ReportParser {

    private static final Logger logger = LoggerFactory.getLogger(ReportParser.class);

    /**
     * Parses the given report and returns the parsed report.
     *
     * @param report The report to parse
     * @return The parsed report
     */
    public ParsedReport parse(Report report) {
        try (CSVParser parser = parserFor(report.content)) {

            List<LetterPrintStatus> statuses =
                stream(parser.spliterator(), false)
                    .map(this::toPrintStatus)
                    .collect(toList());

            return new ParsedReport(
                report.path,
                statuses.stream().filter(Objects::nonNull).collect(toList()),
                statuses.stream().allMatch(Objects::nonNull),
                report.reportDate
            );

        } catch (IOException exc) {
            throw new ReportParsingException(exc);
        }
    }

    /**
     * Creates a CSV parser for the given CSV content.
     * @param csv The CSV content
     * @return The CSV parser
     * @throws IOException If an error occurs while creating the parser
     */
    private CSVParser parserFor(byte[] csv) throws IOException {
        return CSVFormat
            .DEFAULT
            .builder().setHeader().build()
            .parse(new InputStreamReader(new ByteArrayInputStream(csv)));
    }

    /**
     * Converts the given CSV record to a letter print status.
     * @param csvRecord The CSV record
     * @return The letter print status
     */
    private LetterPrintStatus toPrintStatus(CSVRecord csvRecord) {
        try {
            return new LetterPrintStatus(
                FileNameHelper.extractIdFromPdfName(csvRecord.get("InputFileName")),
                ZonedDateTime.parse(csvRecord.get("StartDate") + "T" + csvRecord.get("StartTime") + "Z",
                    DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm'Z'").withZone(ZoneOffset.UTC))
            );
        } catch (Exception exc) {
            logger.error("Error parsing row: {}", csvRecord, exc);
            return null;
        }
    }
}
