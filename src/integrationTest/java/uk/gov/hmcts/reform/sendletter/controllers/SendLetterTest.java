package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.helper.FtpHelper;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ReportParser;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;
import uk.gov.hmcts.reform.sendletter.tasks.MarkLettersPostedTask;
import uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask;
import uk.gov.hmcts.reform.sendletter.util.XeroxReportWriter;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.FileNameHelper;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Posted;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class SendLetterTest {

    @Autowired
    private MockMvc mvc;

    @SpyBean
    private AppInsights insights;

    @Autowired
    private LetterRepository repository;

    @Autowired
    private FtpAvailabilityChecker checker;

    @Test
    public void should_return_200_when_single_letter_is_sent() throws Exception {
        MvcResult result = send(readResource("letter.json"))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotNull();
    }

    @Test
    public void should_upload_letter_and_mark_posted() throws Exception {
        send(readResource("letter.json"))
            .andExpect(status().isOk())
            .andReturn();

        try (LocalSftpServer server = LocalSftpServer.create()) {
            FtpClient client = FtpHelper.getSuccessfulClient(LocalSftpServer.port);

            // Run the upload letters task
            new UploadLettersTask(repository, new Zipper(), client, checker).run();

            // Mimic Xerox and prepare a CSV report based on the PDFs that have been uploaded.
            Stream<UUID> letterIds = Arrays.stream(server.pdfFolder.list()).map(FileNameHelper::extractId);
            XeroxReportWriter.writeReport(letterIds, server.reportFolder);

            // Run the task that processes the Xerox report
            new MarkLettersPostedTask(repository, client, checker, new ReportParser()).run(LocalTime.MIDNIGHT);
        }

        // We should have a single Posted letter.
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll().get(0).getStatus()).isEqualTo(Posted);
    }

    @Test
    public void should_return_400_when_bad_letter_is_sent() throws Exception {
        send("").andExpect(status().isBadRequest());

        verifyNoMoreInteractions(insights);
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
