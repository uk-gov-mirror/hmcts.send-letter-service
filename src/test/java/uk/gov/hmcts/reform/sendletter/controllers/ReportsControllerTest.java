package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.services.ReportsService;

import java.time.LocalDate;
import java.util.Arrays;

import static java.util.Collections.emptyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportsController.class)
class ReportsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportsService reportsService;

    @Test
    void should_return_400_if_date_is_invalid() throws Exception {
        final String invalidDate = "20-19-2019";

        mockMvc
            .perform(get("/reports/count-summary?date=" + invalidDate))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_letters_count_report_in_csv_file() throws Exception {
        given(reportsService.getCountFor(LocalDate.of(2019, 5, 26)))
            .willReturn(Arrays.asList(
                new LettersCountSummary("Service A", 100),
                new LettersCountSummary("Service B", 120)
            ));

        String expectedContent = "Service,Letters Uploaded\r\n"
            + "Service A,100\r\n"
            + "Service B,120\r\n";

        mockMvc
            .perform(get("/reports/count-summary?date=2019-05-26")
                .accept(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(status().isOk())
            .andExpect(header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=letters-count-summary.csv"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(content().string(expectedContent));
    }

    @Test
    void should_return_empty_csv_file_when_service_returns_empty_list() throws Exception {
        given(reportsService.getCountFor(LocalDate.of(2019, 5, 26)))
            .willReturn(emptyList());

        String expectedContent = "Service,Letters Uploaded\r\n";

        mockMvc
            .perform(get("/reports/count-summary?date=2019-05-26")
                .accept(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(status().isOk())
            .andExpect(header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=letters-count-summary.csv"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(content().string(expectedContent));
    }

    @Test
    void should_return_server_error_when_exception_is_thrown() throws Exception {
        given(reportsService.getCountFor(LocalDate.of(2019, 5, 26))).willThrow(RuntimeException.class);

        mockMvc
            .perform(get("/reports/count-summary?date=2019-05-26"))
            .andExpect(status().is5xxServerError());
    }
}
