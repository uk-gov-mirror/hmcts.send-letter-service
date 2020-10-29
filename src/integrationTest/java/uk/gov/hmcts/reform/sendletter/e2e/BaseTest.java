package uk.gov.hmcts.reform.sendletter.e2e;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import org.bouncycastle.openpgp.PGPException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.sendletter.PdfHelper;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.helper.FakeFtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.services.encryption.PgpDecryptionHelper;
import uk.gov.hmcts.reform.sendletter.services.util.FileNameHelper;
import uk.gov.hmcts.reform.sendletter.tasks.MarkLettersPostedTask;
import uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask;
import uk.gov.hmcts.reform.sendletter.tasks.alerts.StaleLettersTask;
import uk.gov.hmcts.reform.sendletter.util.CsvReportWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.atLeastOnce;
import static org.mockito.BDDMockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_FILE_UPLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_REPORT_DELETED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_REPORT_DOWNLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyName.FTP_CLIENT;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyType.FTP;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@DirtiesContext
class BaseTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LetterRepository repository;

    @Autowired
    private FakeFtpAvailabilityChecker fakeFtpAvailabilityChecker;

    @SpyBean
    private TelemetryClient telemetryClient;

    @Captor
    private ArgumentCaptor<RequestTelemetry> requestTelemetryCaptor;

    @Captor
    private ArgumentCaptor<RemoteDependencyTelemetry> dependencyTelemetryCaptor;

    @Autowired
    private WebApplicationContext wac;

    @BeforeEach
    void setUp() {
        WebRequestTrackingFilter filter = new WebRequestTrackingFilter();
        filter.init(new MockFilterConfig());
        mvc = webAppContextSetup(wac).addFilters(filter).build();
        repository.deleteAll();
    }

    @AfterEach
    public void cleanUp() {
        // This test commits transactions to the database
        // so we must clean up afterwards.
        repository.deleteAll();
    }

    void should_upload_letter_and_mark_posted(
        MockHttpServletRequestBuilder request,
        Boolean isEncryptionEnabled
    ) throws Throwable {
        try (LocalSftpServer server = LocalSftpServer.create()) {

            // sftp servers is up, now the background jobs can start connecting to it
            fakeFtpAvailabilityChecker.setAvailable(true);

            mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

            // Wait for letter to be uploaded.
            await()
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    assertThat(server.lettersFolder.listFiles())
                        .as("Files on FTP")
                        .isNotEmpty()
                        .allMatch(f -> isValidPdf(isEncryptionEnabled, f));
                });

            // Generate csv report.
            createCsvReport(server);

            // The report should be processed and the letter marked posted.
            await()
                .atMost(15, SECONDS)
                .untilAsserted(() -> {
                    List<Letter> letters = repository.findAll();
                    assertThat(letters).as("Letters in DB").hasSize(1);
                    assertThat(letters.get(0).getStatus()).as("Letter status").isEqualTo(LetterStatus.Posted);
                });

            // Wait for the csv report to be deleted so that we don't stop the FTP server before the send letters
            // task has finished using it.
            await().atMost(15, SECONDS).untilAsserted(
                () -> assertThat(server.reportFolder.listFiles()).as("CSV reports on FTP").isEmpty()
            );
        }

        verify(telemetryClient, atLeastOnce()).trackRequest(requestTelemetryCaptor.capture());
        assertThat(requestTelemetryCaptor.getAllValues())
            .extracting(requestTelemetry -> tuple(
                requestTelemetry.getName(),
                requestTelemetry.isSuccess()
            ))
            .containsAnyElementsOf(ImmutableList.of(
                tuple("Schedule /" + UploadLettersTask.class.getSimpleName(), true),
                tuple("Schedule /" + MarkLettersPostedTask.class.getSimpleName(), true),
                tuple("Schedule /" + StaleLettersTask.class.getSimpleName(), true)
            ));

        verify(telemetryClient, atLeastOnce()).trackDependency(dependencyTelemetryCaptor.capture());
        assertThat(dependencyTelemetryCaptor.getAllValues())
            .extracting(dependencyTelemetry -> tuple(
                dependencyTelemetry.getType(),
                dependencyTelemetry.getName(),
                dependencyTelemetry.getCommandName(),
                dependencyTelemetry.getSuccess()
            ))
            .containsAnyElementsOf(ImmutableList.of(
                tuple(FTP, FTP_CLIENT, FTP_FILE_UPLOADED, true),
                tuple(FTP, FTP_CLIENT, FTP_REPORT_DELETED, true),
                tuple(FTP, FTP_CLIENT, FTP_REPORT_DOWNLOADED, true)
            ));
    }

    String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }

    private void createCsvReport(LocalSftpServer server) throws IOException {
        try (Stream<UUID> letterIds = Arrays.stream(server.lettersFolder.list())
            .map(FileNameHelper::extractIdFromPdfName)) {

            CsvReportWriter.writeReport(letterIds, server.reportFolder);
        }
    }

    private boolean isValidPdf(boolean isEncryptionEnabled, File file) {
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            if (content.length == 0) {
                // File may still be being written to disk by FTP server.
                return false;
            }
            if (isEncryptionEnabled) {
                // Decrypt encrypted zip file so that we can confirm if we are able to decrypt the encrypted zip
                // using private key and passphrase
                PgpDecryptionHelper.DecryptedFile decryptedFile = PgpDecryptionHelper.decryptFile(
                    content,
                    getClass().getResourceAsStream("/encryption/privatekey.asc"),
                    "Password1".toCharArray()
                );

                assertThat(decryptedFile.filename).endsWith(".zip");
                PdfHelper.validateZippedPdf(decryptedFile.content);

            } else {
                PdfHelper.validateZippedPdf(content);
            }
        } catch (IOException | PGPException e) {
            // File may still be being written to disk by FTP server.
            return false;
        }
        return true;
    }
}
