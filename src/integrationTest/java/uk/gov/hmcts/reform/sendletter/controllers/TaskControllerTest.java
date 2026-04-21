package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.entity.Report;
import uk.gov.hmcts.reform.sendletter.entity.ReportRepository;
import uk.gov.hmcts.reform.sendletter.model.out.CheckPostedTaskResponse;
import uk.gov.hmcts.reform.sendletter.model.out.PostedReportTaskResponse;
import uk.gov.hmcts.reform.sendletter.services.CheckLettersPostedService;
import uk.gov.hmcts.reform.sendletter.services.MarkLettersPostedService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final String API_KEY = "valid-api-key";
    private static final String INVALID_API_KEY = "invalid-api-key";
    private static final String AUTH_HEADER = "Bearer " + API_KEY;
    private static final String INVALID_AUTH_HEADER = "Bearer " + INVALID_API_KEY;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MarkLettersPostedService markLettersPostedService;

    @MockitoBean
    private CheckLettersPostedService checkLettersPostedService;

    @MockitoBean
    private ReportRepository reportRepository;

    @Test
    void processedReportsShouldReturn204WhenEmpty() throws Exception {
        when(reportRepository.findByProcessedAtAfter(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/tasks/processed-reports")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isNoContent());
    }

    @Test
    void processReportsShouldReturn204() throws Exception {
        mockMvc.perform(post("/tasks/process-reports")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isNoContent());
    }

    @Test
    void processedReportsShouldReturn200WithList() throws Exception {
        final LocalDate reportDate = LocalDate.now();
        Report report = Report.builder()
            .reportCode("CODE1")
            .reportDate(reportDate)
            .isInternational(false)
            .printedLettersCount(100)
            .build();

        when(reportRepository.findByProcessedAtAfter(any())).thenReturn(List.of(report));

        String responseBodyStr = mockMvc.perform(get("/tasks/processed-reports")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        List<PostedReportTaskResponse> response = objectMapper.readValue(
            responseBodyStr, new TypeReference<List<PostedReportTaskResponse>>() {});

        assertThat(response).isNotNull().hasSize(1);
        assertThat(response.getFirst()).isEqualTo(PostedReportTaskResponse.fromReport(report));
    }

    @Test
    void checkPostedShouldReturn200() throws Exception {
        CheckPostedTaskResponse item = new CheckPostedTaskResponse(5);
        when(checkLettersPostedService.checkLetters()).thenReturn(item);

        String responseBodyStr = mockMvc.perform(get("/tasks/check-posted")
                .header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        CheckPostedTaskResponse response = objectMapper.readValue(responseBodyStr, CheckPostedTaskResponse.class);
        assertThat(response).isNotNull().isEqualTo(item);
    }

    @Test
    void shouldReturn401WhenMissingApiKey() throws Exception {
        mockMvc.perform(get("/tasks/processed-reports"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenInvalidApiKey() throws Exception {
        // Would normally expect a 403, but historically the api has been
        // written to throw an unauthorised in this situation.
        mockMvc.perform(get("/tasks/processed-reports")
                .header("Authorization", INVALID_AUTH_HEADER))
            .andExpect(status().isUnauthorized());
    }
}
