package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.Report;
import uk.gov.hmcts.reform.sendletter.services.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ReportParser;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(value = MockitoJUnitRunner.class)
public class MarkLettersPostedTest {

    @Mock LetterRepository repo;
    @Mock FtpClient ftpClient;
    @Mock FtpAvailabilityChecker availabilityChecker;
    @Mock ReportParser parser;

    private MarkLettersPostedTask task;

    @Before
    public void setup() {
        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);
        task = new MarkLettersPostedTask(repo, ftpClient, availabilityChecker, parser);
    }

    @Test
    public void continues_processing_if_letter_not_found() throws IOException {
        String filePath = "a.csv";
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(filePath, null)));
        given(parser.parse(any()))
            .willReturn(SampleData.parsedReport(filePath, asList(known, unknown), true));
        Letter letter = SampleData.letterEntity("a.service");
        letter.setStatus(LetterStatus.Uploaded);
        given(repo.findById(known)).willReturn(Optional.of(letter));
        given(repo.findById(unknown)).willReturn(Optional.empty());

        // when
        task.run(LocalTime.MIDNIGHT);

        // then
        assertThat(letter.getStatus()).isEqualTo(LetterStatus.Posted);
    }

    @Test
    public void should_delete_report_if_all_records_were_successfully_parsed() {
        final String reportName = "report.csv";
        final boolean allParsed = true;

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null)));

        given(parser.parse(any())).willReturn(SampleData.parsedReport(reportName, allParsed));

        given(repo.findById(any())).willReturn(Optional.of(SampleData.letterEntity("cmc")));

        // when
        task.run(LocalTime.now());

        // then
        verify(ftpClient).deleteReport(reportName);
    }

    @Test
    public void should_not_delete_report_if_some_records_were_not_successfully_parsed() {
        final String reportName = "report.csv";
        final boolean allParsed = false;

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null)));

        given(parser.parse(any())).willReturn(SampleData.parsedReport(reportName, allParsed));

        given(repo.findById(any())).willReturn(Optional.of(SampleData.letterEntity("cmc")));

        // when
        task.run(LocalTime.now());

        // then
        verify(ftpClient, never()).deleteReport(anyString());
    }
}
