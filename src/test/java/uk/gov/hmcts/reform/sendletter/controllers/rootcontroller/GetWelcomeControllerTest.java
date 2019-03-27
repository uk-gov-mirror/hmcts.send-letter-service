package uk.gov.hmcts.reform.sendletter.controllers.rootcontroller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class GetWelcomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LetterService service; // NOPMD we only need context to load

    @MockBean
    private AuthService authService; // NOPMD we only need context to load

    @Test
    void should_welcome_upon_root_request_with_200_response_code() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string("Welcome to Send Letter Service"));
    }
}
