package uk.gov.hmcts.reform.sendletter.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.exception.FtpException;
import uk.gov.hmcts.reform.sendletter.helper.FtpHelper;
import uk.gov.hmcts.reform.sendletter.services.DuplicateLetterService;
import uk.gov.hmcts.reform.sendletter.services.ExceptionLetterService;
import uk.gov.hmcts.reform.sendletter.services.ExecusionService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;
import uk.gov.hmcts.reform.sendletter.services.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.pdf.DuplexPreparator;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;

import java.io.File;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class UploadLettersTaskTest {

    @Autowired
    LetterRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Mock
    private FtpAvailabilityChecker availabilityChecker;

    @Mock ServiceFolderMapping serviceFolderMapping;

    private LetterService letterService;

    @BeforeEach
    void setUp() {
        when(availabilityChecker.isFtpAvailable(any(LocalTime.class))).thenReturn(true);
        when(serviceFolderMapping.getFolderFor(any())).thenReturn(Optional.of(LocalSftpServer.SERVICE_FOLDER));
        ExecusionService execusionService = new ExecusionService();
        DuplicateLetterService duplicateLetterService = mock(DuplicateLetterService.class);
        ExceptionLetterService exceptionLetterService = mock(ExceptionLetterService.class);

        repository.deleteAll();
        this.letterService = new LetterService(
            new PdfCreator(new DuplexPreparator(), new HTMLToPDFConverter()::convert),
            repository,
            new Zipper(),
            new ObjectMapper(),
            false,
            null,
            serviceFolderMapping,
            execusionService,
            duplicateLetterService,
            exceptionLetterService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false","true"})
    void uploads_file_to_sftp_and_sets_letter_status_to_uploaded(String async) throws Exception {
        UUID id = letterService.save(SampleData.letterRequest(), "bulkprint", async);
        UploadLettersTask task = new UploadLettersTask(
            repository,
            FtpHelper.getSuccessfulClient(LocalSftpServer.port),
            availabilityChecker,
            serviceFolderMapping,
                0
        );

        // Invoke the upload job.
        try (LocalSftpServer server = LocalSftpServer.create()) {
            task.run();

            // file should exist in SFTP site.
            File[] files = server.lettersFolder.listFiles();
            assertThat(files.length).isEqualTo(1);

            // Ensure the letter is marked as uploaded in the database.
            // Clear the JPA cache to force a read.
            entityManager.clear();
            Letter l = repository.findById(id).get();
            assertThat(l.getStatus()).isEqualTo(LetterStatus.Uploaded);
            assertThat(l.getSentToPrintAt()).isNotNull();
            assertThat(l.getPrintedAt()).isNull();
            assertThat(l.getFileContent()).isNotNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"false","true"})
    void should_fail_to_upload_to_sftp_and_stop_from_uploading_any_other_letters(String async) throws Exception {
        // given
        UUID id = letterService.save(SampleData.letterRequest(), "bulkprint", async);
        // additional letter to verify upload loop broke and zipper was never called again
        letterService.save(SampleData.letterRequest(), "bulkprint", async);

        // and
        UploadLettersTask task = new UploadLettersTask(
            repository,
            FtpHelper.getFailingClient(LocalSftpServer.port),
            availabilityChecker,
            serviceFolderMapping,
                0
        );

        // and
        assertThat(repository.findByStatus(LetterStatus.Created)).hasSize(2);

        // when
        try (LocalSftpServer server = LocalSftpServer.create()) {
            assertThat(catchThrowable(task::run)).isInstanceOf(FtpException.class);

            // then
            // file does not exist in SFTP site.
            assertThat(server.lettersFolder.listFiles()).isEmpty();

            // Clear the JPA cache to force a read.
            entityManager.clear();
            Letter l = repository.findById(id).get();
            assertThat(l.getStatus()).isEqualTo(LetterStatus.Created);
            assertThat(l.getSentToPrintAt()).isNull();
            assertThat(l.getFileContent()).isNotNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"false","true"})
    void should_process_only_one_batch_of_files_in_single_run(String async) throws Exception {
        int letterCount = UploadLettersTask.BATCH_SIZE + 1;
        IntStream.rangeClosed(1, letterCount).forEach(
            x -> letterService.save(SampleData.letterRequest(), "bulkprint", async));

        UploadLettersTask task = new UploadLettersTask(
            repository,
            FtpHelper.getSuccessfulClient(LocalSftpServer.port),
            availabilityChecker,
            serviceFolderMapping,
                0
        );

        try (LocalSftpServer server = LocalSftpServer.create()) {
            task.run();
        }
        assertThat(repository.findByStatus(LetterStatus.Uploaded)).hasSize(UploadLettersTask.BATCH_SIZE);
    }
}
