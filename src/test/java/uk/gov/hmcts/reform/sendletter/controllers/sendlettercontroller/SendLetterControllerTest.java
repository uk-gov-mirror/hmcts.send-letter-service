package uk.gov.hmcts.reform.sendletter.controllers.sendlettercontroller;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.sendletter.controllers.MediaTypes;
import uk.gov.hmcts.reform.sendletter.controllers.SendLetterController;
import uk.gov.hmcts.reform.sendletter.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.sendletter.exception.UnauthenticatedException;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SendLetterController.class)
class SendLetterControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private LetterService letterService;
    @MockBean private AuthService authService;


    @Test
    void should_return_message_id_when_letter_is_successfully_sent() throws Exception {
        UUID letterId = UUID.randomUUID();

        given(authService.authenticate("auth-header-value")).willReturn("service-name");
        given(letterService.save(any(LetterRequest.class), anyString())).willReturn(letterId);

        sendLetter(readResource("controller/letter/v1/letter.json"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"letter_id\":" + letterId + "}"));

        verify(authService).authenticate("auth-header-value");
        verify(letterService).save(any(LetterRequest.class), eq("service-name"));
        verifyNoMoreInteractions(authService, letterService);
    }

    @Test
    void should_return_400_client_error_when_invalid_letter_is_sent() throws Exception {
        sendLetter("").andExpect(status().isBadRequest());

        verify(letterService, never()).save(any(LetterRequest.class), anyString());
    }

    @Test
    void should_return_400_client_error_when_letter_is_sent_without_documents() throws Exception {
        sendLetter(readResource("controller/letter/v1/letter-without-doc.json"))
            .andExpect(status().isBadRequest())
            .andExpect(content()
                .json("{\"errors\":[{\"field_name\":\"documents\",\"message\":\"size must be between 1 and 30\"}]}"));

        verify(letterService, never()).save(any(LetterRequest.class), anyString());
    }

    @Test
    void should_return_400_client_error_when_letter_is_sent_without_type() throws Exception {
        sendLetter(readResource("controller/letter/v1/letter-without-type.json"))
            .andExpect(status().isBadRequest())
            .andExpect(content()
                .json("{\"errors\":[{\"field_name\":\"type\",\"message\":\"must not be empty\"}]}"));

        verify(letterService, never()).save(any(LetterRequest.class), anyString());
    }

    @Test
    void should_return_400_client_error_when_letter_is_sent_without_template_in_document() throws Exception {
        sendLetter(readResource("controller/letter/v1/letter-without-template.json"))
            .andExpect(status().isBadRequest())
            .andExpect(content()
                .json("{\"errors\":[{\"field_name\":\"documents[0].template\",\"message\":\"must not be empty\"}]}"));

        verify(letterService, never()).save(any(LetterRequest.class), anyString());
    }

    @Test
    void should_return_400_client_error_when_letter_is_sent_without_template_values_in_document()
        throws Exception {
        sendLetter(readResource("controller/letter/v1/letter-without-template-values.json"))
            .andExpect(status().isBadRequest())
            .andExpect(content()
                .json("{\"errors\":[{\"field_name\":\"documents[0].values\",\"message\":\"must not be empty\"}]}"));

        verify(letterService, never()).save(any(LetterRequest.class), anyString());
    }

    @Test
    void should_return_400_client_error_when_letter_is_with_more_than_30_documents()
        throws Exception {
        sendLetter(readResource("controller/letter/v1/letter-with-multiple-docs.json"))
            .andExpect(status().isBadRequest())
            .andExpect(content()
                .json("{\"errors\":[{\"field_name\":\"documents\",\"message\":\"size must be between 1 and 30\"}]}"));

        verify(letterService, never()).save(any(LetterRequest.class), anyString());
    }

    @Test
    void should_return_401_if_service_auth_header_is_missing() throws Exception {
        given(authService.authenticate(null)).willThrow(new UnauthenticatedException("Hello"));

        MvcResult result = sendLetterWithoutAuthHeader(readResource("controller/letter/v1/letter.json")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void should_return_403_if_service_throws_ServiceNotConfiguredException() throws Exception {
        given(authService.authenticate("auth-header-value")).willReturn("service-name");
        given(letterService.save(any(), any())).willThrow(new ServiceNotConfiguredException("invalid service"));

        sendLetter(readResource("controller/letter/v1/letter.json"))
            .andExpect(status().isForbidden());
    }

    @Test
    void should_support_two_content_types() throws Exception {
        given(authService.authenticate(anyString())).willReturn("my_service");

        List<String> supportedContentTypes = asList(
            MediaTypes.LETTER_V1,
            MediaType.APPLICATION_JSON_VALUE
        );

        for (String type : supportedContentTypes) {
            mockMvc
                .perform(
                    post("/letters")
                        .contentType(type)
                        .header("ServiceAuthorization", "auth-header-value")
                        .content(readResource("controller/letter/v1/letter.json"))
                )
                .andExpect(status().isOk());
        }

        verify(letterService, times(supportedContentTypes.size()))
            .save(any(LetterRequest.class), anyString());
    }

    private ResultActions sendLetter(String json) throws Exception {
        return mockMvc.perform(
            post("/letters")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("ServiceAuthorization", "auth-header-value")
                .content(json)
        );
    }

    private ResultActions sendLetterWithoutAuthHeader(String json) throws Exception {
        return mockMvc.perform(
            post("/letters")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
        );
    }

    private String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
}
