package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.model.out.StaleLetter;
import uk.gov.hmcts.reform.sendletter.services.StaleLetterService;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaleLetterController.class)
public class StaleLetterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaleLetterService staleLetterService;

    @Test
    public void should_return_letters_from_stale_letter_service() throws Exception {
        given(staleLetterService.getStaleLetters()).willReturn(Stream.of(
            new StaleLetter(
                UUID.fromString("767cf17e-0ec0-452b-a457-bc173d51ff40"),
                "status1",
                "service1",
                LocalDateTime.parse("2019-05-03T12:34:56.123"),
                LocalDateTime.parse("2019-05-03T13:00:00.000"),
                false
            ),
            new StaleLetter(
                UUID.fromString("462a58fe-e9b7-494f-a719-5083f31c69cf"),
                "status2",
                "service2",
                LocalDateTime.parse("2019-05-02T10:11:22.321"),
                null,
                true
            )
        ));

        mockMvc
            .perform(get("/stale-letters").accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(
                content().json(
                    Resources.toString(
                        getResource("controller/stale-letters/stale-letters-response.json"),
                        UTF_8
                    ),
                    false
                )
            );
    }

    @Test
    public void should_return_server_error_when_stale_letter_service_throws_exception() throws Exception {
        given(staleLetterService.getStaleLetters()).willThrow(RuntimeException.class);

        mockMvc
            .perform(
                get("/stale-letters")
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().is5xxServerError());
    }
}