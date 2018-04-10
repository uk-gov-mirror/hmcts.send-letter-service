package uk.gov.hmcts.reform.sendletter.e2e;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sendletter.PdfHelper;
import uk.gov.hmcts.reform.sendletter.controllers.MediaTypes;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.logging.AppDependency;
import uk.gov.hmcts.reform.sendletter.logging.AppDependencyCommand;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.logging.Dependency;
import uk.gov.hmcts.reform.sendletter.services.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.util.XeroxReportWriter;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.FileNameHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = "scheduling.enabled=true")
public class EndToEndTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LetterRepository repository;

    @SpyBean
    private AppInsights insights;

    @After
    public void cleanUp() {
        // This test commits transactions to the database
        // so we must clean up afterwards.
        repository.deleteAll();
    }

    @Test
    public void should_handle_old_letter_model() throws Throwable {
        should_upload_letter_and_mark_posted(
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readResource("letter.json"))
        );
    }

    @Test
    public void should_handle_new_letter_model() throws Throwable {
        should_upload_letter_and_mark_posted(
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaTypes.LETTER_V2)
                .content(readResource("letter-with-pdf.json"))
        );
    }

    private void should_upload_letter_and_mark_posted(MockHttpServletRequestBuilder request) throws Throwable {
        ArgumentCaptor<Dependency> dependencyCaptor = ArgumentCaptor.forClass(Dependency.class);

        try (LocalSftpServer server = LocalSftpServer.create()) {
            mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

            // Wait for letter to be uploaded.
            await().atMost(15, SECONDS).untilAsserted(
                () -> assertThat(server.lettersFolder.listFiles()).as("No letters uploaded!").isNotEmpty()
            );

            // Generate Xerox report.
            createXeroxReport(server);

            // The report should be processed and the letter marked posted.
            await().atMost(15, SECONDS).untilAsserted(
                () -> assertThat(letterHasBeenPosted()).as("Letter not posted").isTrue()
            );
        }

        verify(insights, atLeastOnce()).trackDependency(any(ProceedingJoinPoint.class), dependencyCaptor.capture());

        List<Dependency> dependencies = dependencyCaptor.getAllValues();
        List<String> dependencyValues = dependencies.stream().map(Dependency::value).collect(Collectors.toList());
        List<String> dependencyCommands = dependencies.stream().map(Dependency::command).collect(Collectors.toList());

        assertThat(dependencyValues).contains(AppDependency.FTP_CLIENT);
        assertThat(dependencyCommands).contains(
            AppDependencyCommand.FTP_DOWNLOAD_REPORTS,
            AppDependencyCommand.FTP_FILE_UPLOADED,
            AppDependencyCommand.FTP_REPORT_DELETE
        );
    }

    private boolean letterHasBeenPosted() {
        List<Letter> letters = repository.findAll();
        return letters.size() == 1 && letters.get(0).getStatus() == LetterStatus.Posted;
    }

    private void createXeroxReport(LocalSftpServer server) throws IOException {
        Stream<UUID> letterIds = Arrays.stream(server.lettersFolder.listFiles()).map(this::validateGettingLetterId);
        XeroxReportWriter.writeReport(letterIds, server.reportFolder);
    }

    // Validate that the file is a zipped PDF,
    // returning the Letter's ID from the filename.
    private UUID validateGettingLetterId(File file) {
        try {
            PdfHelper.validateZippedPdf(Files.readAllBytes(file.toPath()));
            return FileNameHelper.extractId(file.getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
}
