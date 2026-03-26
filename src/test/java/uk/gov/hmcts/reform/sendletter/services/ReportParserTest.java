package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.exception.ReportParsingException;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintStatus;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;
import uk.gov.hmcts.reform.sendletter.model.Report;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.sendletter.util.ResourceLoader.loadResource;

class ReportParserTest {

    private static final UUID UUID_1 = UUID.fromString("edcc9e2d-eddb-4d87-9a16-61f4499f524c");
    private static final UUID UUID_2 = UUID.fromString("edcc9e2d-eddb-4d87-9a16-61f4499f524d");
    private static final ZonedDateTime expectedZonedDateTime = ZonedDateTime.of(
        2018, 3, 27, 16, 38, 0, 0,
        ZoneId.of("Z"));

    @Test
    void should_parse_valid_csv_report() {
        String report = formatReport(UUID_1, UUID_2);
        ParsedReport result = new ReportParser().parse(
            new Report("a.csv", report.getBytes(), ZonedDateTime.now().toEpochSecond()));

        assertThat(result.statuses)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new LetterPrintStatus(UUID_1, expectedZonedDateTime),
                new LetterPrintStatus(UUID_2, expectedZonedDateTime)
            );
        assertThat(result.allRowsParsed).isTrue();
    }

    @Test
    void should_filter_out_rows_with_invalid_file_name() {
        String report = formatReport("invalidID", UUID_1);
        ParsedReport result = new ReportParser().parse(
            new Report("a.csv", report.getBytes(), ZonedDateTime.now().toEpochSecond()));

        assertThat(result.statuses)
            .usingFieldByFieldElementComparator()
            .containsExactly(new LetterPrintStatus(UUID_1, expectedZonedDateTime));

        assertThat(result.allRowsParsed).isFalse();
    }

    @Test
    void should_filter_out_rows_with_invalid_date() {
        String report =
            "\"StartDate\",\"StartTime\",\"InputFileName\"\n"
                + "20180101,16:38,CMC001_cmcclaimstore_ff99f8ad-7ab8-43f8-9671-5397cbfa96a6.pdf\n"
                + String.format("27-03-2018,16:38,CMC001_cmcclaimstore_%s\n", UUID_1);

        ParsedReport result = new ReportParser().parse(
            new Report("a.csv", report.getBytes(),  ZonedDateTime.now().toEpochSecond()));

        assertThat(result.statuses)
            .usingFieldByFieldElementComparator()
            .containsExactly(new LetterPrintStatus(UUID_1, expectedZonedDateTime));

        assertThat(result.allRowsParsed).isFalse();
    }

    @Test
    void should_parse_sample_report() throws Exception {
        byte[] report = loadResource("report.csv");

        ParsedReport result = new ReportParser().parse(
            new Report("a.csv", report, ZonedDateTime.now().toEpochSecond()));

        assertThat(result.statuses).hasSize(3);
        assertThat(result.allRowsParsed).isTrue();
    }

    @Test
    void should_throw_report_parsing_exception_when_csv_contains_semicolon_delimiter() {
        String report =
            "\"StartDate\";\"StartTime\";\"InputFileName\"\n"
                + "27-03-2018;16:38;CMC001_cmcclaimstore_ff99f8ad-7ab8-43f8-9671-5397cbfa96a6.pdf\n"
                + "27-03-2018;16:38;CMC001_cmcclaimstore_ff88f8ad-8ab8-44f8-9672-5398cbfa96a7.pdf\n";

        Throwable exc = catchThrowable(() ->
            new ReportParser().parse(new Report("a.csv", report.getBytes(), ZonedDateTime.now().toEpochSecond())));

        assertThat(exc)
            .isInstanceOf(ReportParsingException.class);
    }

    private String formatReport(Object... ids) {
        StringBuilder report = new StringBuilder(
            "\"StartDate\",\"StartTime\",\"InputFileName\"\n");
        for (Object id : ids) {
            report.append(formatRecord(id));
        }
        return report.toString();
    }

    private String formatRecord(Object id) {
        return String.format("27-03-2018,16:38,CMC001_claimstore_%s.pdf\n", id);
    }
}
