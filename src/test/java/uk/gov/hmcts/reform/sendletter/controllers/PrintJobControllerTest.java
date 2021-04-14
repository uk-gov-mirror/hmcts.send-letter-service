package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.sendletter.exception.UnauthenticatedException;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrintJobController.class)
class PrintJobControllerTest {

    @MockBean
    private SasTokenGeneratorService sasTokenGeneratorService;

    @MockBean
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_sasResponse() throws Exception {
        given(authService.authenticate("auth-header-value")).willReturn("send-letter");
        given(sasTokenGeneratorService.generateSasToken("send-letter")).willReturn("sas_token");

        mockMvc
            .perform(
                get("/token")
                    .header("ServiceAuthorization", "auth-header-value")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sas_token").value("sas_token"))
            .andExpect(jsonPath("$.request_id").isNotEmpty());

        verify(authService).authenticate("auth-header-value");
        verify(sasTokenGeneratorService).generateSasToken("send-letter");
    }

    @Test
    void should_return_unauthorized_status_when_authentication_fails() throws Exception {
        given(authService.authenticate("auth-header-value"))
            .willThrow(
                new UnauthenticatedException("Missing ServiceAuthorization header")
            );

        mockMvc
            .perform(
                get("/token")
                    .header("ServiceAuthorization", "auth-header-value")
            )
            .andDo(print())
            .andExpect(status().isUnauthorized());

        verify(authService).authenticate("auth-header-value");
        verify(sasTokenGeneratorService, never()).generateSasToken(any());
    }

    @Test
    void should_return_forbidden_status_when_service_is_not_configured() throws Exception {
        given(authService.authenticate("auth-header-value")).willReturn("send-letter");
        given(sasTokenGeneratorService.generateSasToken("send-letter"))
            .willThrow(
                new ServiceNotConfiguredException("Service not configured")
            );

        mockMvc
            .perform(
                get("/token")
                    .header("ServiceAuthorization", "auth-header-value")
            )
            .andDo(print())
            .andExpect(status().isForbidden());

        verify(authService).authenticate("auth-header-value");
        verify(sasTokenGeneratorService).generateSasToken("send-letter");
    }

}
