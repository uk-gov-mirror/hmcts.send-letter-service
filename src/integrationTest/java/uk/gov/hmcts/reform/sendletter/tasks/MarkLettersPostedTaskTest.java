package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;
import uk.gov.hmcts.reform.sendletter.helper.FtpHelper;
import uk.gov.hmcts.reform.slc.services.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.slc.services.ReportParser;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.FtpClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class MarkLettersPostedTaskTest {

    @Autowired
    LetterRepository repository;

    @Autowired
    private EntityManager entityManager;

    ReportParser parser = new ReportParser();
    FtpAvailabilityChecker checker = mock(FtpAvailabilityChecker.class);
    final LocalDateTime printedAt = LocalDateTime.parse("2018-01-01T10:30:53");

    @Test
    public void marks_uploaded_letters_as_posted() throws Exception {
        // Create a letter in the Uploaded state.
        Letter letter = SampleData.letterEntity("testService");
        letter.setState(LetterState.Uploaded);
        repository.saveAndFlush(letter);

        when(checker.isFtpAvailable(any(LocalTime.class))).thenReturn(true);
        try (LocalSftpServer server = LocalSftpServer.create()) {
            FtpClient client = FtpHelper.getClient(LocalSftpServer.port);
            MarkLettersPostedTask task = new MarkLettersPostedTask(repository, client, checker, parser);

            // Prepare the response CSV from Xerox and run the task.
            writeXeroxCsvForLetter(letter, server.reportFolder);
            task.run();
        }

        // Check that the letter has moved to the Posted state.
        entityManager.flush();
        letter = repository.findById(letter.getId()).get();
        assertThat(letter.getState()).isEqualTo(LetterState.Posted);
        // Check that the printedAt date has been set.
        assertThat(letter.getPrintedAt().toLocalDateTime()).isEqualTo(printedAt);
    }

    // Prepare a CSV in the format provided by Xerox for a given letter
    // and put it in the reports sftp folder.
    private void writeXeroxCsvForLetter(Letter l, File reportFolder) throws IOException {
        String content =
            "\"Date\",\"Time\",\"Filename\"\n"
                + String.format("%s,%s,first_second_%s\n",
                DateTimeFormatter.ofPattern("yyyy-MM-dd").format(printedAt),
                DateTimeFormatter.ofPattern("hh:mm:ss").format(printedAt),
                l.getId());
        File report = new File(reportFolder, l.getMessageId() + ".csv");
        report.createNewFile();
        Files.write(report.toPath(), content.getBytes());
    }
}
