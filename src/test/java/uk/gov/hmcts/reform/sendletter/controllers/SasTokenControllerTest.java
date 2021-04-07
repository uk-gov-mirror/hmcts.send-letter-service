package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SasTokenController.class)
class SasTokenControllerTest {

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

        mockMvc.perform(
                get("/token")
                    .header("ServiceAuthorization", "auth-header-value")
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sas_token").value("sas_token"))
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }
}
