package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.sendletter.entity.Report;
import uk.gov.hmcts.reform.sendletter.entity.ReportRepository;
import uk.gov.hmcts.reform.sendletter.entity.ReportStatus;
import uk.gov.hmcts.reform.sendletter.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.sendletter.model.out.CheckPostedTaskResponse;
import uk.gov.hmcts.reform.sendletter.model.out.PostedReportTaskResponse;
import uk.gov.hmcts.reform.sendletter.services.CheckLettersPostedService;
import uk.gov.hmcts.reform.sendletter.services.MarkLettersPostedService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private static final String API_KEY = "test-key";
    private static final String AUTH_HEADER = "Bearer " + API_KEY;

    @Mock
    private MarkLettersPostedService markLettersPostedService;

    @Mock
    private CheckLettersPostedService checkLettersPostedService;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private TaskController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new TaskController(
            API_KEY,
            markLettersPostedService,
            checkLettersPostedService,
            reportRepository
        );
    }

    @Test
    void processReportsShouldReturn204() {
        when(markLettersPostedService.processReports()).thenReturn(Collections.emptyList());

        ResponseEntity<Void> response =
            controller.processReports(AUTH_HEADER);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.NO_CONTENT.value());
        assertThat(response.hasBody()).isFalse();
    }

    @Test
    void retrieveReportsShouldReturn200WithList() {
        Report report = Report.builder()
            .reportName("Test Report")
            .reportCode("CODE1")
            .reportDate(LocalDate.now())
            .printedLettersCount(10)
            .isInternational(false)
            .status(ReportStatus.SUCCESS)
            .build();

        when(reportRepository.findByProcessedAtAfter(any()))
            .thenReturn(List.of(report));

        ResponseEntity<List<PostedReportTaskResponse>> response =
            controller.retrieveReports(AUTH_HEADER, LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.hasBody()).isTrue();

        List<PostedReportTaskResponse> body = response.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.getFirst().getReportCode()).isEqualTo(report.getReportCode());
        assertThat(body.getFirst().getReportDate()).isEqualTo(report.getReportDate());
        assertThat(body.getFirst().isInternational()).isEqualTo(report.isInternational());
        assertThat(body.getFirst().getMarkedPostedCount()).isEqualTo(report.getPrintedLettersCount());
        assertThat(body.getFirst().isProcessingFailed()).isFalse();
    }

    @Test
    void checkPostedShouldReturn200WithResponse() {
        CheckPostedTaskResponse expected = new CheckPostedTaskResponse(5);
        when(checkLettersPostedService.checkLetters()).thenReturn(expected);

        ResponseEntity<CheckPostedTaskResponse> response =
            controller.checkPosted(AUTH_HEADER);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.hasBody()).isTrue();
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test()
    void shouldThrowExceptionWhenApiKeyMissing() {
        assertThatThrownBy(() -> controller.checkPosted(null))
            .isInstanceOf(InvalidApiKeyException.class)
            .hasMessageContaining("API Key is missing");
    }

    @Test
    void shouldThrowException_whenApiKeyInvalid() {
        assertThatThrownBy(() -> controller.checkPosted("Bearer NONSENSE"))
            .isInstanceOf(InvalidApiKeyException.class)
            .hasMessageContaining("Invalid API Key");
    }
}
