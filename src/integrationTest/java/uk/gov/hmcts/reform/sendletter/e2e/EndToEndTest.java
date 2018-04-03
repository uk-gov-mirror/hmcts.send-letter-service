package uk.gov.hmcts.reform.sendletter.e2e;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.util.XeroxReportWriter;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.FileNameHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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

    @After
    public void cleanUp() {
        // This test commits transactions to the database
        // so we must clean up afterwards.
        repository.deleteAll();
    }

    @Test
    public void should_upload_letter_and_mark_posted() throws Exception {
        try (LocalSftpServer server = LocalSftpServer.create()) {
            send(readResource("letter.json"))
                .andExpect(status().isOk())
                .andReturn();

            // Wait for pdfs to be uploaded.
            await().atMost(60, SECONDS).untilAsserted(
                () -> assertThat(server.pdfFolder.listFiles()).as("No PDFs uploaded!").isNotEmpty());

            // Generate Xerox report.
            createXeroxReport(server);

            // The report should be processed and the letter marked posted.
            await().atMost(60, SECONDS).untilAsserted(
                () -> assertThat(letterHasBeenPosted()).as("Letter not posted").isTrue());
        }
    }

    private boolean letterHasBeenPosted() {
        List<Letter> letters = repository.findAll();
        return letters.size() == 1 && letters.get(0).getStatus() == LetterStatus.Posted;
    }

    private void createXeroxReport(LocalSftpServer server) throws IOException {
        Stream<UUID> letterIds = Arrays.stream(server.pdfFolder.list()).map(FileNameHelper::extractId);
        XeroxReportWriter.writeReport(letterIds, server.reportFolder);
    }

    private ResultActions send(String content) throws Exception {
        MockHttpServletRequestBuilder request =
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        return mvc.perform(request);
    }

    private String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
}
