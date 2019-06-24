package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.config.TimeConfiguration;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@SpringBootTest
@Transactional
class GetLetterStatusTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LetterRepository letterRepository;

    @MockBean
    private AuthTokenValidator tokenValidator;

    @AfterEach
    void tearDown() {
        letterRepository.deleteAll();
    }

    @Test
    void should_return_200_when_matching_letter_found_in_db() throws Exception {
        // given
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("some-service");

        // and
        Letter letter = SampleData.letterEntity("some-service");
        letterRepository.saveAndFlush(letter);

        getLetterStatus(letter.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(letter.getId().toString()))
            .andExpect(jsonPath("$.status").value(letter.getStatus().name()))
            .andExpect(jsonPath("$.checksum").value(letter.getChecksum()))
            .andExpect(jsonPath("$.created_at").value(toIso(letter.getCreatedAt())))
            .andExpect(jsonPath("$.sent_to_print_at").isEmpty())
            .andExpect(jsonPath("$.printed_at").isEmpty())
            .andExpect(jsonPath("$.has_failed").value(false));
    }

    @Test
    void should_return_404_when_letter_is_not_found() throws Exception {
        getLetterStatus(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    private ResultActions getLetterStatus(UUID letterId) throws Exception {
        return mvc.perform(
            get("/letters/" + letterId.toString())
                .header("ServiceAuthorization", "auth-header-value")
        );
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime
            .atZone(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ofPattern(TimeConfiguration.DATE_TIME_PATTERN));
    }
}
