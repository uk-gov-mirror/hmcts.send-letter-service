package uk.gov.hmcts.reform.sendletter.services;

import org.junit.Test;
import uk.gov.hmcts.reform.slc.model.LetterPrintStatus;
import uk.gov.hmcts.reform.slc.services.ReportParser;
import uk.gov.hmcts.reform.slc.services.ReportParsingException;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.ParsedReport;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.Report;

import java.time.ZonedDateTime;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ReportParserTest {

    private static final UUID UUID_1 = UUID.fromString("edcc9e2d-eddb-4d87-9a16-61f4499f524c");
    private static final UUID UUID_2 = UUID.fromString("edcc9e2d-eddb-4d87-9a16-61f4499f524d");

    @Test
    public void should_parse_valid_csv_report() {
        String report = formatReport(UUID_1, UUID_2);
        ParsedReport result = new ReportParser().parse(new Report("a.csv", report.getBytes()));

        assertThat(result.statuses)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new LetterPrintStatus(UUID_1, ZonedDateTime.parse("2018-01-01T10:30:53Z")),
                new LetterPrintStatus(UUID_2, ZonedDateTime.parse("2018-01-01T10:30:53Z"))
            );
    }

    @Test
    public void should_filter_out_rows_with_invalid_file_name() {
        String report = formatReport("invalidID", UUID_1);
        ParsedReport result = new ReportParser().parse(new Report("a.csv", report.getBytes()));

        assertThat(result.statuses)
            .usingFieldByFieldElementComparator()
            .containsExactly(new LetterPrintStatus(UUID_1, ZonedDateTime.parse("2018-01-01T10:30:53Z")));
    }

    @Test
    public void should_filter_out_rows_with_invalid_date() {
        String report =
            "\"Date\",\"Time\",\"Filename\"\n"
                + "20180101,10:30:53,TE5A_TE5B_9364001\n"
                + String.format("2018-01-01,10:30:53,TE5A_TE5B_%s\n", UUID_1);

        ParsedReport result = new ReportParser().parse(new Report("a.csv", report.getBytes()));

        assertThat(result.statuses)
            .usingFieldByFieldElementComparator()
            .containsExactly(new LetterPrintStatus(UUID_1, ZonedDateTime.parse("2018-01-01T10:30:53Z")));
    }

    @Test
    public void should_parse_sample_report() throws Exception {
        byte[] report = toByteArray(getResource("report.csv"));

        ParsedReport result = new ReportParser().parse(new Report("a.csv", report));

        assertThat(result.statuses).hasSize(11);
    }

    @Test
    public void should_throw_report_parsing_exception_when_csv_contains_semicolon_delimiter() {
        String report =
            "\"Date\";\"Time\";\"Filename\"\n"
                + "20180101;10:30:53;TE5A_TE5B_9364001\n"
                + "2018-01-01;10:30:53;TE5A_TE5B_9364002\n";

        Throwable exc = catchThrowable(() ->
            new ReportParser().parse(new Report("a.csv", report.getBytes())));

        assertThat(exc)
            .isInstanceOf(ReportParsingException.class);
    }

    private String formatReport(Object... ids) {
        StringBuffer report = new StringBuffer(
            "\"Date\",\"Time\",\"Filename\"\n");
        for (Object id : ids) {
            report.append(formatRecord(id));
        }
        return report.toString();
    }

    private String formatRecord(Object id) {
        return String.format("2018-01-01,10:30:53,TE5A_TE5B_%s\n", id);
    }
}
