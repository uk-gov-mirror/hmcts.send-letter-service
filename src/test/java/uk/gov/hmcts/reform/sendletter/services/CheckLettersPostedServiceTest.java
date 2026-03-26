package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.entity.Report;
import uk.gov.hmcts.reform.sendletter.entity.ReportRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.out.CheckPostedTaskResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckLettersPostedServiceTest {

    @Mock
    StaleLetterService staleLetterService;
    @Mock
    LetterService letterService;
    @Mock
    LetterActionService letterActionService;
    @Mock
    ReportRepository reportRepository;
    @Mock
    ReportsServiceConfig reportsServiceConfig;

    @Mock
    uk.gov.hmcts.reform.sendletter.model.out.LetterStatus letterStatus;

    @InjectMocks
    private CheckLettersPostedService checkLettersPostedService;

    @Test
    void shouldReturnZeroResultWhenReportsExistForAllStaleLetters() {

        Report report = Report.builder().build();

        List<BasicLetterInfo> letters = createStaleLetters(5, "some_service_name");
        when(staleLetterService.getStaleLettersWithValidPrintDate(anyList(), any(LocalDateTime.class)))
            .thenReturn(letters);
        when(letterService.getStatus(any(UUID.class), eq("true"), eq("false")))
            .thenReturn(letterStatus);
        when(reportsServiceConfig.getReportCode(any(), any())).thenReturn("CODE1");
        when(reportRepository.findFirstByReportCodeAndReportDateAndIsInternational(any(), any(), anyBoolean()))
            .thenReturn(Optional.of(report));

        CheckPostedTaskResponse response = checkLettersPostedService.checkLetters();

        assertThat(response)
            .isNotNull()
            .extracting("markedNoReportAbortedCount").isEqualTo(0);
    }

    @Test
    void shouldReturnCorrectResultWhenOnlySomeLettersHaveAssociatedReports() {

        final Report report = Report.builder().build();

        List<BasicLetterInfo> letters = createStaleLetters(1, "some_service_name");
        letters.addAll(createStaleLetters(4, "another_service_name"));
        when(staleLetterService.getStaleLettersWithValidPrintDate(anyList(), any(LocalDateTime.class)))
            .thenReturn(letters);
        when(letterService.getStatus(any(UUID.class), eq("true"), eq("false")))
            .thenReturn(letterStatus);
        when(reportsServiceConfig.getReportCode(eq("some_service_name"), any())).thenReturn("CODE1");
        when(reportsServiceConfig.getReportCode(eq("another_service_name"), any())).thenReturn("CODE2");
        when(reportRepository.findFirstByReportCodeAndReportDateAndIsInternational(
            eq("CODE1"), any(LocalDate.class), anyBoolean())).thenReturn(Optional.of(report));
        when(reportRepository.findFirstByReportCodeAndReportDateAndIsInternational(
            eq("CODE2"), any(LocalDate.class), anyBoolean())).thenReturn(Optional.empty());
        when(letterActionService.markLetterAsNoReportAborted(any())).thenReturn(1);

        CheckPostedTaskResponse response = checkLettersPostedService.checkLetters();

        // expect all but the first letter to be marked aborted.
        assertThat(response)
            .isNotNull()
            .extracting("markedNoReportAbortedCount").isEqualTo(letters.size() - 1);
    }

    @Test
    void shouldTreatUnknownReportCodeAsAssociatedReportLookupFailure() {

        final Report report = Report.builder().build();

        List<BasicLetterInfo> letters = createStaleLetters(1, "some_service_name");
        letters.addAll(createStaleLetters(4, "another_service_name"));
        when(staleLetterService.getStaleLettersWithValidPrintDate(anyList(), any(LocalDateTime.class)))
            .thenReturn(letters);
        when(letterService.getStatus(any(UUID.class), eq("true"), eq("false")))
            .thenReturn(letterStatus);
        when(reportsServiceConfig.getReportCode(eq("some_service_name"), any())).thenReturn("CODE1");
        when(reportsServiceConfig.getReportCode(eq("another_service_name"), any())).thenReturn(null);
        when(reportRepository.findFirstByReportCodeAndReportDateAndIsInternational(
            eq("CODE1"), any(LocalDate.class), anyBoolean())).thenReturn(Optional.of(report));
        when(letterActionService.markLetterAsNoReportAborted(any())).thenReturn(1);

        CheckPostedTaskResponse response = checkLettersPostedService.checkLetters();

        // expect all but the first letter to be marked aborted.
        assertThat(response)
            .isNotNull()
            .extracting("markedNoReportAbortedCount").isEqualTo(letters.size() - 1);
    }

    @Test
    void shouldIgnoreUnknownLetters() {

        final Report report = Report.builder().build();

        List<BasicLetterInfo> letters = createStaleLetters(1, "some_service_name");
        letters.addAll(createStaleLetters(4, "another_service_name"));
        when(staleLetterService.getStaleLettersWithValidPrintDate(anyList(), any(LocalDateTime.class)))
            .thenReturn(letters);
        when(letterService.getStatus(any(UUID.class), eq("true"), eq("false")))
            .thenReturn(letterStatus);
        when(reportsServiceConfig.getReportCode(eq("some_service_name"), any())).thenReturn("CODE1");
        when(reportsServiceConfig.getReportCode(eq("another_service_name"), any()))
            .thenThrow(LetterNotFoundException.class);
        when(reportRepository.findFirstByReportCodeAndReportDateAndIsInternational(
            eq("CODE1"), any(LocalDate.class), anyBoolean())).thenReturn(Optional.of(report));

        CheckPostedTaskResponse response = checkLettersPostedService.checkLetters();

        // expect no letters to be aborted as there was either a report, or the letter couldn't be found
        assertThat(response)
            .isNotNull()
            .extracting("markedNoReportAbortedCount").isEqualTo(0);
    }

    private List<BasicLetterInfo> createStaleLetters(int count, String serviceName) {
        List<BasicLetterInfo> letters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            letters.add(new BasicLetterInfo(
                UUID.randomUUID(),
                "",
                serviceName,
                LetterStatus.Uploaded,
                "",
                "",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
            ));
        }
        return letters;
    }
}
