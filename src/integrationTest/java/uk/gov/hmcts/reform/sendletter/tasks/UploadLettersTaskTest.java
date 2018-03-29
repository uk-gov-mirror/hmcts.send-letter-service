package uk.gov.hmcts.reform.sendletter.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.schmizz.sshj.xfer.LocalSourceFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.exception.DocumentZipException;
import uk.gov.hmcts.reform.sendletter.helper.FtpHelper;
import uk.gov.hmcts.reform.sendletter.services.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.LetterService;
import uk.gov.hmcts.reform.sendletter.services.zip.ZippedDoc;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfDoc;

import java.io.File;
import java.time.LocalTime;
import java.util.UUID;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class UploadLettersTaskTest {

    @Autowired
    LetterRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Mock
    private FtpAvailabilityChecker availabilityChecker;

    @Before
    public void setUp() {
        when(availabilityChecker.isFtpAvailable(any(LocalTime.class))).thenReturn(true);
    }

    @Test
    public void uploads_file_to_sftp_and_sets_letter_status_to_uploaded() throws Exception {
        LetterService s = new LetterService(repository, new ObjectMapper());
        UUID id = s.send(SampleData.letter(), "service");
        UploadLettersTask task = new UploadLettersTask(
            repository,
            new Zipper(),
            FtpHelper.getSuccessfulClient(LocalSftpServer.port),
            availabilityChecker
        );

        // Invoke the upload job.
        try (LocalSftpServer server = LocalSftpServer.create()) {
            task.run();

            // file should exist in SFTP site.
            File[] files = server.pdfFolder.listFiles();
            assertThat(files.length).isEqualTo(1);

            // Ensure the letter is marked as uploaded in the database.
            // Clear the JPA cache to force a read.
            entityManager.clear();
            Letter l = repository.findById(id).get();
            assertThat(l.getStatus()).isEqualTo(LetterStatus.Uploaded);
            assertThat(l.getSentToPrintAt()).isNotNull();
            assertThat(l.getPrintedAt()).isNull();

            // pdf content should be removed now
            assertThat(l.getPdf()).isNull();
        }
    }

    @Test
    public void should_fail_to_upload_to_sftp_and_stop_from_uploading_any_other_letters() throws Exception {
        // given
        LetterService s = new LetterService(repository, new ObjectMapper());
        UUID id = s.send(SampleData.letter(), "service");
        // additional letter to verify upload loop broke and zipper was never called again
        s.send(SampleData.letter(), "service");

        // and
        Zipper fakeZipper = mock(Zipper.class);
        when(fakeZipper.zip(anyString(), any(PdfDoc.class))).thenReturn(new ZippedDoc("test.zip", new byte[0]));

        UploadLettersTask task = new UploadLettersTask(
            repository,
            fakeZipper,
            FtpHelper.getFailingClient(LocalSftpServer.port),
            availabilityChecker
        );

        // and
        assertThat(repository.findByStatus(LetterStatus.Created).count()).isEqualTo(2);

        // when
        try (LocalSftpServer server = LocalSftpServer.create()) {
            task.run();

            // then
            // verify zipper was called only once
            verify(fakeZipper).zip(anyString(), any(PdfDoc.class));

            // file does not exist in SFTP site.
            File[] files = server.pdfFolder.listFiles();
            assertThat(files.length).isEqualTo(0);

            // Clear the JPA cache to force a read.
            entityManager.clear();
            Letter l = repository.findById(id).get();
            assertThat(l.getStatus()).isEqualTo(LetterStatus.Created);
            assertThat(l.getSentToPrintAt()).isNull();
            assertThat(l.getPdf()).isNotNull();
        }
    }

    @Test
    @SuppressWarnings("VariableDeclarationUsageDistance")
    public void should_fail_to_zip_causing_no_changes_to_letter_and_ftp_miss() throws Exception {
        LetterService s = new LetterService(repository, new ObjectMapper());
        UUID id = s.send(SampleData.letter(), "service");
        Zipper failingZipper = mock(Zipper.class);
        FtpClient ftpClient = mock(FtpClient.class);

        UploadLettersTask task = new UploadLettersTask(
            repository,
            failingZipper,
            ftpClient,
            availabilityChecker
        );

        doThrow(DocumentZipException.class).when(failingZipper).zip(anyString(), any(PdfDoc.class));

        task.run();

        // Clear the JPA cache to force a read.
        entityManager.clear();
        Letter l = repository.findById(id).get();
        assertThat(l.getStatus()).isEqualTo(LetterStatus.Created);
        assertThat(l.getSentToPrintAt()).isNull();
        assertThat(l.getPdf()).isNotNull();
        verify(ftpClient, never()).upload(any(LocalSourceFile.class), anyBoolean());
    }
}
