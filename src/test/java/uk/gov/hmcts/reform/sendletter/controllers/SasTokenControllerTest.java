package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(SasTokenController.class)
class SasTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_sasResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/token/send-letter"))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.sas_token").isNotEmpty())
        .andExpect(MockMvcResultMatchers.jsonPath("$.request_id").isNotEmpty());
    }
}
