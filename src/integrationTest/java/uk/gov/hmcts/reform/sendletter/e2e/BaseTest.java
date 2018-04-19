package uk.gov.hmcts.reform.sendletter.e2e;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.bouncycastle.openpgp.PGPException;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sendletter.PdfHelper;
import uk.gov.hmcts.reform.sendletter.config.ThreadPoolConfig;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.services.encryption.PgpDecryptionHelper;
import uk.gov.hmcts.reform.sendletter.services.util.FileNameHelper;
import uk.gov.hmcts.reform.sendletter.util.XeroxReportWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@SpringBootTest
@DirtiesContext
public class BaseTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LetterRepository repository;

    @After
    public void cleanUp() {
        // This test commits transactions to the database
        // so we must clean up afterwards.
        repository.deleteAll();
    }

    protected void should_upload_letter_and_mark_posted(
        MockHttpServletRequestBuilder request,
        Boolean isEncryptionEnabled
    ) throws Throwable {
        try (LocalSftpServer server = LocalSftpServer.create()) {
            mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

            // Wait for letter to be uploaded.
            await().atMost(15, SECONDS).untilAsserted(
                () -> assertThat(server.lettersFolder.listFiles()).as("No letters uploaded!").isNotEmpty()
            );

            // Generate Xerox report.
            createXeroxReport(server, isEncryptionEnabled);

            // The report should be processed and the letter marked posted.
            await().atMost(15, SECONDS).untilAsserted(
                () -> assertThat(letterHasBeenPosted()).as("Letter not posted").isTrue()
            );

            // Wait for the Xerox report to be deleted so that we don't stop the FTP server before the send letters
            // task has finished using it.
            await().atMost(15, SECONDS).untilAsserted(
                () -> assertThat(server.reportFolder.listFiles()).as("Xerox reports not deleted!").isEmpty()
            );

            assertThat(ThreadPoolConfig.getUnhandledTaskExceptionCount())
                .as("Scheduled tasks encountered unhandled exceptions!").isZero();
        }
    }

    protected String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }

    private boolean letterHasBeenPosted() {
        List<Letter> letters = repository.findAll();
        return letters.size() == 1 && letters.get(0).getStatus() == LetterStatus.Posted;
    }

    private void createXeroxReport(LocalSftpServer server, Boolean isEncryptionEnabled) throws IOException {
        try (Stream<UUID> letterIds = Arrays.stream(server.lettersFolder.listFiles())
            .map(file -> validateGettingLetterId(file, isEncryptionEnabled))) {

            XeroxReportWriter.writeReport(letterIds, server.reportFolder);
        }
    }

    // returning the Letter's ID from the filename.
    private UUID validateGettingLetterId(File file, Boolean isEncryptionEnabled) {
        try {
            if (isEncryptionEnabled) {
                // Decrypt encrypted zip file so that we can confirm if we are able to decrypt the encrypted zip
                // using private key and passphrase
                PgpDecryptionHelper.decryptFile(
                    Files.readAllBytes(file.toPath()),
                    getClass().getResourceAsStream("/encryption/privatekey.asc"),
                    "Password1".toCharArray()
                );
            } else {
                PdfHelper.validateZippedPdf(Files.readAllBytes(file.toPath()));
            }

            return FileNameHelper.extractIdFromPdfName(file.getName());
        } catch (IOException | PGPException e) {
            throw new RuntimeException(e);
        }
    }
}
