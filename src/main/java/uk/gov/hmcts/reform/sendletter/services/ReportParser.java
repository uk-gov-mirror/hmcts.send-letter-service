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

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

@Component
public class ReportParser {

    private static final Logger logger = LoggerFactory.getLogger(ReportParser.class);

    public ParsedReport parse(Report report) {
        try (CSVParser parser = parserFor(report.content)) {

            List<LetterPrintStatus> statuses =
                stream(parser.spliterator(), false)
                    .map(this::toPrintStatus)
                    .collect(toList());

            return new ParsedReport(
                report.path,
                statuses.stream().filter(status -> status != null).collect(toList()),
                statuses.stream().allMatch(status -> status != null)
            );

        } catch (IOException exc) {
            throw new ReportParsingException(exc);
        }
    }

    private CSVParser parserFor(byte[] csv) throws IOException {
        return CSVFormat
            .DEFAULT
            .withHeader()
            .parse(new InputStreamReader(new ByteArrayInputStream(csv)));
    }

    /**
     * Converts cvs row into a letter print status object.
     * Returns null if conversion fails.
     */
    private LetterPrintStatus toPrintStatus(CSVRecord record) {
        try {
            return new LetterPrintStatus(
                FileNameHelper.extractIdFromPdfName(record.get("InputFileName")),
                ZonedDateTime.parse(record.get("StartDate") + "T" + record.get("StartTime") + "Z",
                    DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm'Z'").withZone(ZoneOffset.UTC))
            );
        } catch (Exception exc) {
            logger.error("Error parsing row: {}", record, exc);
            return null;
        }
    }
}
