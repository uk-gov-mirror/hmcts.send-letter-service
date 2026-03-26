package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.entity.ReportRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;
import uk.gov.hmcts.reform.sendletter.model.Report;
import uk.gov.hmcts.reform.sendletter.model.out.PostedReportTaskResponse;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarkLettersPostedTest {

    @Mock LetterDataAccessService dataAccessService;
    @Mock LetterService letterService;
    @Mock FtpClient ftpClient;
    @Mock FtpAvailabilityChecker availabilityChecker;
    @Mock ReportParser parser;
    @Mock AppInsights insights;
    @Mock ReportsServiceConfig reportsServiceConfig;
    @Mock ReportRepository reportRepository;

    private MarkLettersPostedService task;

    @BeforeEach
    void setup() {
        task = new MarkLettersPostedService(
            dataAccessService,
            letterService,
            ftpClient,
            availabilityChecker,
            parser,
            insights,
            reportsServiceConfig,
            reportRepository
        );
    }

    @Test
    void continues_processing_if_letter_not_found() {
        String filePath = "MOJ_CMC_domestic.csv";
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);
        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(filePath, null, Instant.now().getEpochSecond())));
        given(parser.parse(any()))
            .willReturn(SampleData.parsedReport(filePath, asList(known, unknown), true));
        Letter letter = SampleData.letterEntity("a.service");
        letter.setStatus(LetterStatus.Uploaded);
        given(dataAccessService.findLetterStatus(known)).willReturn(Optional.of(letter.getStatus()));
        given(dataAccessService.findLetterStatus(unknown)).willReturn(Optional.empty());
        given(reportsServiceConfig.getReportCodes()).willReturn(Set.of("CMC"));

        // when
        task.processReports();

        // then
        verify(dataAccessService).markLetterAsPosted(eq(known), any(LocalDateTime.class));
        verify(dataAccessService, never()).markLetterAsPosted(eq(unknown), any(LocalDateTime.class));
    }

    @Test
    void should_delete_report_if_all_records_were_successfully_parsed() {
        final String reportName = "MOJ_CMC_domestic.csv";
        final boolean allParsed = true;

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null, Instant.now().getEpochSecond())));

        given(parser.parse(any())).willReturn(SampleData.parsedReport(reportName, allParsed));

        given(dataAccessService.findLetterStatus(any()))
            .willReturn(Optional.of(SampleData.letterEntity("cmc").getStatus()));

        given(reportsServiceConfig.getReportCodes()).willReturn(Set.of("CMC"));

        // when
        task.processReports();

        // then
        verify(ftpClient).deleteReport(reportName);
    }

    @Test
    void should_not_delete_report_if_some_records_were_not_successfully_parsed() {
        final String reportName = "MOJ_CMC_domestic.csv";
        final boolean allParsed = false;

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null, Instant.now().getEpochSecond())));

        given(parser.parse(any())).willReturn(SampleData.parsedReport(reportName, allParsed));

        given(dataAccessService.findLetterStatus(any()))
            .willReturn(Optional.of(SampleData.letterEntity("cmc").getStatus()));

        given(reportsServiceConfig.getReportCodes()).willReturn(Set.of("CMC"));

        // when
        task.processReports();

        // then
        verify(ftpClient, never()).deleteReport(anyString());
    }

    @Test
    void should_not_attempt_to_download_reports_during_ftp_downtime() {
        given(availabilityChecker.isFtpAvailable(any())).willReturn(false);

        // when
        task.processReports();

        // then
        verify(ftpClient, never()).downloadReports();
    }

    // ------------------------------------------------------
    // -- test additions after migration from task to service

    @Test
    void should_not_delete_report_and_return_error_response_if_report_id_could_not_be_identified() {
        final String reportName = "MOJ_NOTAVALIDREPORTID.csv";
        final boolean allParsed = true;

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null, Instant.now().getEpochSecond())));

        given(parser.parse(any())).willReturn(SampleData.parsedReport(reportName, allParsed));

        given(dataAccessService.findLetterService(any()))
            .willReturn(Optional.of("not_a_proper_service"));

        given(reportsServiceConfig.getReportCodes()).willReturn(Set.of("CODE1", "CODE2"));

        // when
        List<PostedReportTaskResponse> processedReports = task.processReports();

        // then
        verify(ftpClient, never()).deleteReport(reportName);
        assertThat(processedReports).isNotNull().isNotEmpty().hasSize(1);
        assertThat(processedReports.getFirst()).satisfies(p -> {
            assertThat(p.isProcessingFailed()).isTrue();
            assertThat(p.getMarkedPostedCount()).isZero();
            assertThat(p.getErrorMessage()).isNotEmpty().contains(
                String.format("Service not found for report with name '%s'",  reportName)
            );
        });
    }

    @Test
    void should_delete_report_and_return_success_response_if_report_id_can_only_be_identified_from_letters() {
        final String reportName = "POORLY_NAMED_REPORT.csv";
        final boolean allParsed = true;

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null, Instant.now().getEpochSecond())));

        ParsedReport parsedReport = SampleData.parsedReport(reportName, allParsed);
        given(parser.parse(any())).willReturn(parsedReport);

        given(dataAccessService.findLetterService(any()))
            .willReturn(Optional.of("some_service_name"));

        given(letterService.getStatus(any(), eq(Boolean.TRUE.toString()), eq(Boolean.FALSE.toString())))
            .willReturn(new uk.gov.hmcts.reform.sendletter.model.out.LetterStatus(
                UUID.randomUUID(),
                "some_service_status",
                "",
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                null,
                null
            ));

        given(reportsServiceConfig.getReportCode(eq("some_service_name"),
            any(uk.gov.hmcts.reform.sendletter.model.out.LetterStatus.class)))
            .willReturn("CODE1");

        given(dataAccessService.findLetterStatus(any())).willReturn(Optional.of(LetterStatus.Uploaded));

        // when
        List<PostedReportTaskResponse> processedReports = task.processReports();

        // then
        verify(dataAccessService, times(parsedReport.statuses.size()))
            .markLetterAsPosted(any(),any(LocalDateTime.class));
        verify(ftpClient).deleteReport(reportName);
        verify(reportRepository).save(any());
        assertThat(processedReports).isNotNull().isNotEmpty().hasSize(1);
        assertThat(processedReports.getFirst()).satisfies(p -> {
            assertThat(p.isProcessingFailed()).isFalse();
            // SampleData parsedReport has 2 letters in it
            assertThat(p.getMarkedPostedCount()).isEqualTo(parsedReport.statuses.size());
            assertThat(p.getErrorMessage()).isNull();
            assertThat(p.getReportCode()).isEqualTo("CODE1");
        });
    }

    @Test
    void should_not_delete_report_and_return_error_response_when_mark_posted_fails() {
        final String reportName = "MOJ_CODE1_domestic.csv";
        final boolean allParsed = true;

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null, Instant.now().getEpochSecond())));

        ParsedReport parsedReport = SampleData.parsedReport(reportName, allParsed);
        given(parser.parse(any())).willReturn(parsedReport);

        given(reportsServiceConfig.getReportCodes()).willReturn(Set.of("CODE1", "CODE2"));
        given(dataAccessService.findLetterStatus(any())).willReturn(Optional.of(LetterStatus.Uploaded));

        // set the mark letters posted task up to explode
        willThrow(new RuntimeException("test exception message"))
            .given(dataAccessService)
            .markLetterAsPosted(any(UUID.class),any(LocalDateTime.class));

        // when
        List<PostedReportTaskResponse> processedReports = task.processReports();

        // then
        verify(ftpClient, never()).deleteReport(reportName);
        verify(reportRepository, never()).save(any());
        assertThat(processedReports).isNotNull().isNotEmpty().hasSize(1);
        assertThat(processedReports.getFirst()).satisfies(p -> {
            assertThat(p.isProcessingFailed()).isTrue();
            assertThat(p.getMarkedPostedCount()).isZero();
            assertThat(p.getErrorMessage()).isNotEmpty().contains(
                "An error occurred when processing downloaded reports from the SFTP server: test exception message"
            );
        });
    }

    @Test
    void should_resolve_report_id_when_only_some_letters_in_db() {
        final String reportName = "MOJ_NOTAVALIDREPORTID.csv";
        final boolean allParsed = true;

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null, Instant.now().getEpochSecond())));

        ParsedReport parsedReport = SampleData.parsedReport(reportName, allParsed);
        given(parser.parse(any())).willReturn(parsedReport);

        given(dataAccessService.findLetterService(parsedReport.statuses.getFirst().id))
            .willThrow(new LetterNotFoundException(parsedReport.statuses.getFirst().id));

        given(dataAccessService.findLetterService(parsedReport.statuses.getLast().id))
            .willReturn(Optional.of("some_service_name"));

        given(letterService.getStatus(any(), eq(Boolean.TRUE.toString()), eq(Boolean.FALSE.toString())))
            .willReturn(new uk.gov.hmcts.reform.sendletter.model.out.LetterStatus(
                UUID.randomUUID(),
                "some_service_status",
                "",
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                null,
                null
            ));

        given(reportsServiceConfig.getReportCodes()).willReturn(Set.of("CODE1", "CODE2"));
        given(reportsServiceConfig.getReportCode(eq("some_service_name"),
            any(uk.gov.hmcts.reform.sendletter.model.out.LetterStatus.class)))
            .willReturn("CODE1");

        given(dataAccessService.findLetterStatus(any())).willReturn(Optional.of(LetterStatus.Uploaded));

        // when
        List<PostedReportTaskResponse> processedReports = task.processReports();

        // then
        verify(ftpClient).deleteReport(reportName);
        verify(reportRepository).save(any());
        assertThat(processedReports).isNotNull().isNotEmpty().hasSize(1);
        assertThat(processedReports.getFirst().getReportCode()).isEqualTo("CODE1");
    }


    @ParameterizedTest
    @ValueSource(strings = {"MOJ_CODE1_domestic-13-01-2026.csv","MOJ_CODE1_domestic-2026-01-13.csv"})
    void should_resolve_report_date_from_report_filename_if_present(String reportName) {
        final boolean allParsed = true;

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null, Instant.now().getEpochSecond())));

        ParsedReport parsedReport = SampleData.parsedReport(reportName, allParsed);
        given(parser.parse(any())).willReturn(parsedReport);

        given(reportsServiceConfig.getReportCodes()).willReturn(Set.of("CODE1", "CODE2"));

        // when
        List<PostedReportTaskResponse> processedReports = task.processReports();

        // then
        verify(ftpClient).deleteReport(reportName);
        verify(reportRepository).save(any());
        assertThat(processedReports).isNotNull().isNotEmpty().hasSize(1);
        assertThat(processedReports.getFirst().getReportDate()).isEqualTo(LocalDate.of(2026,1,13));
    }

    @Test
    void should_resolve_report_date_from_parsed_report_when_filename_date_is_malformed() {
        final String reportName = "MOJ_CODE1_domestic-99-76-2026.csv";
        final boolean allParsed = true;

        given(availabilityChecker.isFtpAvailable(any())).willReturn(true);

        given(ftpClient.downloadReports())
            .willReturn(singletonList(new Report(reportName, null, Instant.now().getEpochSecond())));

        ParsedReport parsedReport = SampleData.parsedReport(reportName, allParsed);
        given(parser.parse(any())).willReturn(parsedReport);

        given(reportsServiceConfig.getReportCodes()).willReturn(Set.of("CODE1", "CODE2"));

        // when
        List<PostedReportTaskResponse> processedReports = task.processReports();

        // then
        verify(ftpClient).deleteReport(reportName);
        verify(reportRepository).save(any());
        assertThat(processedReports).isNotNull().isNotEmpty().hasSize(1);
        assertThat(processedReports.getFirst().getReportDate()).isEqualTo(parsedReport.reportDate);
    }

}
