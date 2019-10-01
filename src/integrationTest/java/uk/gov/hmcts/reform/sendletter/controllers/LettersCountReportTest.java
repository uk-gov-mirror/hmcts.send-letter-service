package uk.gov.hmcts.reform.sendletter.controllers;

import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@SpringBootTest
class LettersCountReportTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LetterRepository letterRepository;

    @MockBean
    private AuthTokenValidator tokenValidator;

    @Autowired
    private WebApplicationContext wac;

    @BeforeEach
    void setUp() {
        WebRequestTrackingFilter filter = new WebRequestTrackingFilter();
        filter.init(new MockFilterConfig());
        mvc = webAppContextSetup(wac).addFilters(filter).build();
    }

    @AfterEach
    void tearDown() {
        letterRepository.deleteAll();
    }

    @Test
    void should_return_letters_count_summary_and_filter_the_test_service() throws Exception {
        // given
        Letter letter1 = SampleData.letterEntity("some_service_name");
        letter1.setSentToPrintAt(LocalDateTime.of(2019, 4, 26, 14, 30, 10));

        Letter letter2 = SampleData.letterEntity("send_letter_tests");
        letter2.setSentToPrintAt(LocalDateTime.of(2019, 4, 26, 10, 30, 10));

        letterRepository.saveAll(Arrays.asList(letter1, letter2));

        MvcResult mvcResult = mvc.perform(get("/reports/count-summary?date=2019-04-26"))
            .andExpect(status().isOk())
            .andExpect(header()
                .string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=letters-count-summary.csv"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString())
            .isNotNull()
            .contains("Service,Letters Uploaded") // Report header
            .contains("Some Service,1") // Expected count for service
            .doesNotContain("Bulk Print,1"); // Test service should be filtered
    }

}
