package uk.gov.hmcts.reform.sendletter.controllers.reports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.entity.Report;
import uk.gov.hmcts.reform.sendletter.entity.ReportRepository;

import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class CheckReportsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportsServiceConfig reportsServiceConfig;

    @MockitoBean
    private AuthTokenValidator tokenValidator;

    @AfterEach
    void tearDown() {
        reportRepository.deleteAll();
    }

    @Test
    void should_return_200_when_all_reports_are_present() throws Exception {
        // given
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 7);
        Set<String> reportCodes = reportsServiceConfig.getReportCodes();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (String code : reportCodes) {
                reportRepository.save(Report.builder()
                    .reportName("Test " + code + " Domestic")
                    .reportCode(code)
                    .reportDate(date)
                    .isInternational(false)
                    .build());
                reportRepository.save(Report.builder()
                    .reportName("Test " + code + " International")
                    .reportCode(code)
                    .reportDate(date)
                    .isInternational(true)
                    .build());
            }
        }

        // when
        mvc.perform(get("/reports/check-reports")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
            // then
            .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_parameters_are_missing_and_reports_exist_for_today() throws Exception {
        // given
        LocalDate today = LocalDate.now();
        Set<String> reportCodes = reportsServiceConfig.getReportCodes();

        for (String code : reportCodes) {
            reportRepository.save(Report.builder()
                .reportName("Test " + code + " Domestic")
                .reportCode(code)
                .reportDate(today)
                .isInternational(false)
                .build());
            reportRepository.save(Report.builder()
                .reportName("Test " + code + " International")
                .reportCode(code)
                .reportDate(today)
                .isInternational(true)
                .build());
        }

        // when
        mvc.perform(get("/reports/check-reports"))
            // then
            .andExpect(status().isOk());
    }

    @Test
    void should_return_404_when_reports_are_missing() throws Exception {
        // given
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 1);
        Set<String> reportCodes = reportsServiceConfig.getReportCodes();
        String code = reportCodes.iterator().next(); // Use the first code

        // Save only domestic report
        reportRepository.save(Report.builder()
            .reportName("Test " + code + " Domestic")
            .reportCode(code)
            .reportDate(startDate)
            .isInternational(false)
            .build());

        // when
        mvc.perform(get("/reports/check-reports")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
            // then
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$[?(@.service_name == '%s' && @.is_international == true)]", code).exists())
            .andExpect(jsonPath("$[?(@.report_date == '%s')]", startDate.toString()).exists());
    }
}
