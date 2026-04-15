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
        LocalDate date = LocalDate.of(2021, 1, 1);
        Set<String> reportCodes = reportsServiceConfig.getReportCodes();

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

        // when
        mvc.perform(get("/reports/check-reports")
                .param("startDate", "2021-01-01")
                .param("endDate", "2021-01-01"))
            // then
            .andExpect(status().isOk());
    }

    @Test
    void should_return_404_when_reports_are_missing() throws Exception {
        // given
        LocalDate date = LocalDate.of(2021, 1, 1);
        Set<String> reportCodes = reportsServiceConfig.getReportCodes();
        String code = reportCodes.iterator().next(); // Use the first code

        // Save only domestic report
        reportRepository.save(Report.builder()
            .reportName("Test " + code + " Domestic")
            .reportCode(code)
            .reportDate(date)
            .isInternational(false)
            .build());

        // when
        mvc.perform(get("/reports/check-reports")
                .param("startDate", "2021-01-01")
                .param("endDate", "2021-01-01"))
            // then
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$[?(@.serviceName == '%s' && @.type == 'international')]", code).exists());
    }
}
