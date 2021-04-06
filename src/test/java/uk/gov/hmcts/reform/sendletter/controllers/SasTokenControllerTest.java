package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SasTokenController.class)
class SasTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_sasResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/token/send-letter"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sas_token").isNotEmpty())
            .andExpect(jsonPath("$.request_id").isNotEmpty());
    }
}
