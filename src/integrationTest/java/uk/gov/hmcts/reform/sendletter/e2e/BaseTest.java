package uk.gov.hmcts.reform.sendletter.e2e;

import com.google.common.collect.ImmutableList;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.bouncycastle.openpgp.PGPException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.sendletter.PdfHelper;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.helper.FakeFtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.out.PostedReportTaskResponse;
import uk.gov.hmcts.reform.sendletter.services.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.services.MarkLettersPostedService;
import uk.gov.hmcts.reform.sendletter.services.encryption.PgpDecryptionHelper;
import uk.gov.hmcts.reform.sendletter.services.util.FileNameHelper;
import uk.gov.hmcts.reform.sendletter.util.CsvReportWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.atLeastOnce;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_FILE_UPLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_REPORT_DELETED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_REPORT_DOWNLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyName.FTP_CLIENT;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyType.FTP;


@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BaseTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LetterRepository repository;

    @Autowired
    MarkLettersPostedService markLettersPostedService;

    @Autowired
    private FakeFtpAvailabilityChecker fakeFtpAvailabilityChecker;

    @MockitoSpyBean
    private AppInsights insights;

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private final TelemetryContext context = new TelemetryContext();

    @Captor
    private ArgumentCaptor<RequestTelemetry> requestTelemetryCaptor;

    @Captor
    private ArgumentCaptor<RemoteDependencyTelemetry> dependencyTelemetryCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(insights, "telemetryClient", telemetryClient);
        context.setInstrumentationKey("some-key");
        when(telemetryClient.getContext()).thenReturn(context);
        //Must clean up before tests as repository from last controller test does not clear itself properly
        repository.deleteAll();
    }

    @AfterEach
    public void cleanUp() {
        // This test commits transactions to the database
        // so we must clean up afterwards
        repository.deleteAll();
        reset(telemetryClient);
    }

    void shouldUploadLetterAndMarkPosted(
        MockHttpServletRequestBuilder request,
        Boolean isEncryptionEnabled
    ) throws Throwable {
        try (var server = LocalSftpServer.create()) {

            // sftp servers is ups, now the background jobs can start connecting to it
            fakeFtpAvailabilityChecker.setAvailable(true);
            System.out.println("REQUEST: " + request.toString());
            mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

            // Wait for letter to be uploaded.
            await()
                .forever()
                .untilAsserted(() -> {
                    assertThat(server.lettersFolder.listFiles())
                        .as("Files on FTP")
                        .isNotEmpty()
                        .allMatch(f -> isValidPdf(isEncryptionEnabled, f));
                });

            // Generate csv report.
            createCsvReport(server);

            // Run the process-report task manually as it is no longer a scheduled task
            await()
                .forever()
                .untilAsserted(() -> {
                    List<PostedReportTaskResponse> mpResp = markLettersPostedService.processReports();
                    assertThat(mpResp).isNotNull().isNotEmpty().hasSize(1);
                    assertThat(mpResp.getFirst()).isNotNull().extracting("processingFailed").isEqualTo(false);
                });

            // The report should be processed and the letter marked posted.
            await()
                .forever()
                .untilAsserted(() -> {
                    List<Letter> letters = repository.findAll();
                    assertThat(letters).as("Letters in DB").hasSize(1);
                    assertThat(letters.get(0).getStatus()).as("Letter status").isEqualTo(LetterStatus.Posted);
                });

            // Wait for the csv report to be deleted so that we don't stop the FTP server before the send letters
            // task has finished using it.
            await().forever().untilAsserted(
                () -> assertThat(server.reportFolder.listFiles()).as("CSV reports on FTP").isEmpty()
            );
        }

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
        return StreamUtils.copyToString(
            new ClassPathResource(fileName).getInputStream(), UTF_8);
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
                // using private key and passphrase.
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
