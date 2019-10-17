package uk.gov.hmcts.reform.sendletter.controllers.sendlettercontroller;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.sendletter.controllers.MediaTypes;
import uk.gov.hmcts.reform.sendletter.controllers.SendLetterController;
import uk.gov.hmcts.reform.sendletter.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsAndNumberOfCopiesRequest;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SendLetterController.class)
public class SendLetterWithPdfsAndCopiesControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private LetterService letterService;
    @MockBean private AuthService authService;

    @Test
    void should_call_service() throws Exception {
        given(authService.authenticate(anyString())).willReturn("some_service_name");

        // when
        sendLetter(Resources.toString(getResource("controller/letter/v3/valid_letter.json"), UTF_8));

        // then
        verify(letterService).save(any(LetterWithPdfsAndNumberOfCopiesRequest.class), anyString());
    }

    @Test
    void should_authenticate_calls() throws Exception {
        given(authService.authenticate(anyString())).willReturn("some_service_name");
        final String authHeader = "auth-header-value";

        // when
        mockMvc.perform(
            post("/letters")
                .contentType(MediaTypes.LETTER_V3)
                .header("ServiceAuthorization", authHeader)
                .content(Resources.toString(getResource("controller/letter/v3/valid_letter.json"), UTF_8))
        );

        // then
        verify(authService).authenticate(eq(authHeader));
    }

    @Test
    void should_validate_number_of_copies() throws Exception {
        given(authService.authenticate(anyString())).willReturn("some_service_name");

        sendLetter(Resources.toString(getResource("controller/letter/v3/invalid_copies.json"), UTF_8))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_403_if_service_throws_ServiceNotConfiguredException() throws Exception {
        given(authService.authenticate(anyString())).willReturn("some_service_name");
        given(letterService.save(any(), any())).willThrow(new ServiceNotConfiguredException("invalid service"));

        sendLetter(Resources.toString(getResource("controller/letter/v3/valid_letter.json"), UTF_8))
            .andExpect(status().isForbidden());
    }

    private ResultActions sendLetter(String json) throws Exception {
        return mockMvc.perform(
            post("/letters")
                .contentType(MediaTypes.LETTER_V3)
                .header("ServiceAuthorization", "auth-header-value")
                .content(json)
        );
    }
}
