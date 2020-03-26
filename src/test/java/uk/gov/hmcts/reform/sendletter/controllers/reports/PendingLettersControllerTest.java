package uk.gov.hmcts.reform.sendletter.controllers.reports;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.services.PendingLettersService;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.Month.MAY;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PendingLettersController.class)
public class PendingLettersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PendingLettersService service;

    @Test
    void should_return_pending_letters() throws Exception {

        BasicLetterInfo letter1 = letter(
            UUID.fromString("9b9c019d-78e7-479d-afc6-a7b5b585c47e"),
            "service_A",
            LocalDateTime.of(2019, MAY, 17, 11, 29, 30),
            "9c61b7da4e6c94416be51136122ed01acea9884f"
        );
        BasicLetterInfo letter2 = letter(
            UUID.fromString("19ae96e9-30c2-4b8b-befc-7d26c7fb0731"),
            "service_B",
            LocalDateTime.of(2019, MAY, 16, 7, 0, 0),
            "85e38f69046b44c1ec9fb07b76d78f0500d026c4"
        );

        given(service.getPendingLetters()).willReturn(asList(letter1, letter2));

        mockMvc
            .perform(get("/pending-letters"))
            .andExpect(status().isOk())
            .andExpect(content().json(
                "{"
                    + "'pending_letters': ["
                    + "  {"
                    + "    'id': '9b9c019d-78e7-479d-afc6-a7b5b585c47e',"
                    + "    'service': 'service_A',"
                    + "    'created_at': '2019-05-17T11:29:30',"
                    + "    'key_fingerprint': '9c61b7da4e6c94416be51136122ed01acea9884f'"
                    + "  },"
                    + "  {"
                    + "    'id': '19ae96e9-30c2-4b8b-befc-7d26c7fb0731',"
                    + "    'service': 'service_B',"
                    + "    'created_at': '2019-05-16T07:00:00',"
                    + "    'key_fingerprint': '85e38f69046b44c1ec9fb07b76d78f0500d026c4'"
                    + "  }"
                    + "]"
                    + "}"
                )
            );
    }

    @Test
    void should_return_empty_model_when_there_are_no_pending_envelopes() throws Exception {

        given(service.getPendingLetters()).willReturn(emptyList());

        mockMvc
            .perform(get("/pending-letters"))
            .andExpect(status().isOk())
            .andExpect(content().json("{ 'pending_letters': [] }"));
    }

    private BasicLetterInfo letter(UUID id, String service, LocalDateTime createdAt, String keyFingerprint) {
        BasicLetterInfo letter = mock(BasicLetterInfo.class);
        when(letter.getId()).thenReturn(id);
        when(letter.getService()).thenReturn(service);
        when(letter.getCreatedAt()).thenReturn(createdAt);
        when(letter.getEncryptionKeyFingerprint()).thenReturn(keyFingerprint);
        return letter;
    }
}
