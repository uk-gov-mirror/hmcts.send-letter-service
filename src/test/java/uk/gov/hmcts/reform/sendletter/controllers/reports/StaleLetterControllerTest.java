package uk.gov.hmcts.reform.sendletter.controllers.reports;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.StaleLetterService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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
        List<BasicLetterInfo> letters = Arrays.asList(
            letter(
                UUID.fromString("767cf17e-0ec0-452b-a457-bc173d51ff40"),
                "service1",
                LetterStatus.Uploaded,
                LocalDateTime.parse("2019-05-03T12:34:56.123"),
                LocalDateTime.parse("2019-05-03T13:00:00.000")
            ),
            letter(
                UUID.fromString("462a58fe-e9b7-494f-a719-5083f31c69cf"),
                "service2",
                LetterStatus.Created,
                LocalDateTime.parse("2019-05-02T10:11:22.321"),
                null
            )
        );

        given(staleLetterService.getStaleLetters()).willReturn(letters);

        mockMvc
            .perform(get("/stale-letters").accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
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
                    .accept(APPLICATION_JSON_VALUE)
            )
            .andExpect(status().is5xxServerError());
    }

    private BasicLetterInfo letter(
        UUID id,
        String service,
        LetterStatus status,
        LocalDateTime createdAt,
        LocalDateTime sentToPrintAt
    ) {
        return new BasicLetterInfo(id, "checksum", service, status, "type", null, createdAt, sentToPrintAt, null);
    }
}
