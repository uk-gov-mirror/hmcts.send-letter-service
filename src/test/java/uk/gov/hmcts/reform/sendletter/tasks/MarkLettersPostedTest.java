package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;
import uk.gov.hmcts.reform.sendletter.services.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.FtpClient;
import uk.gov.hmcts.reform.slc.model.LetterPrintStatus;
import uk.gov.hmcts.reform.slc.services.ReportParser;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.ParsedReport;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.Report;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

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
            .willReturn(
                new ParsedReport(
                    filePath,
                    asList(
                        new LetterPrintStatus(unknown, now()),
                        new LetterPrintStatus(known, now())
                    )
                )
            );
        Letter letter = SampleData.letterEntity("a.service");
        letter.setState(LetterState.Uploaded);
        given(repo.findById(known)).willReturn(Optional.of(letter));
        given(repo.findById(unknown)).willReturn(Optional.empty());
        task.run(LocalTime.MIDNIGHT);
        assertThat(letter.getState()).isEqualTo(LetterState.Posted);
    }
}
