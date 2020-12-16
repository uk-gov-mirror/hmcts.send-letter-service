package uk.gov.hmcts.reform.sendletter.controllers.sendlettercontroller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.sendletter.controllers.SendLetterController;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.v2.LetterStatusV2;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SendLetterController.class)
class GetLetterStatusControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private LetterService service;
    @MockBean private AuthService authService;

    private LetterStatus letterStatus;


    @BeforeEach
    void setUp() {
        ZonedDateTime now = ZonedDateTime.of(2000, 2, 12, 1, 2, 3, 123_000_000, ZoneId.systemDefault());
        letterStatus = new LetterStatus(UUID.randomUUID(), "Created",
                "some-message-id", now, now, now, null, null);
    }

    @Test
    void should_return_letter_status_when_it_is_found_in_database() throws Exception {

        given(service.getStatus(letterStatus.id, "true", "false")).willReturn(letterStatus);

        getLetter(letterStatus.id, "true", "false")
            .andExpect(status().isOk())
            .andExpect(content().json(
                "{"
                    + "\"id\":\"" + letterStatus.id.toString() + "\","
                    + "\"message_id\":\"" + letterStatus.messageId + "\","
                    + "\"checksum\":\"" + letterStatus.checksum + "\","
                    + "\"created_at\":\"2000-02-12T01:02:03.123Z\","
                    + "\"sent_to_print_at\":\"2000-02-12T01:02:03.123Z\","
                    + "\"printed_at\":\"2000-02-12T01:02:03.123Z\","
                    + "\"copies\":null"
                    + "}"
            ));
    }

    @Test
    void should_return_letter_status_when_it_is_found_in_database_with_v2_copies() throws Exception {
        Map<String, Object> detailCopies = Map.of("Document_1", 1);
        ZonedDateTime now = ZonedDateTime.of(2000, 2, 12, 1, 2, 3, 123_000_000, ZoneId.systemDefault());

        LetterStatusV2 letterStatus =
                new LetterStatusV2(UUID.randomUUID(), "Created",
                "some-message-id", now, now, now, null, detailCopies);
        given(service.getLatestStatus(letterStatus.id)).willReturn(letterStatus);

        mockMvc.perform(
                get("/letters/v2/" + letterStatus.id))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "\"id\":\"" + letterStatus.id.toString() + "\","
                                + "\"message_id\":\"" + letterStatus.messageId + "\","
                                + "\"checksum\":\"" + letterStatus.checksum + "\","
                                + "\"created_at\":\"2000-02-12T01:02:03.123Z\","
                                + "\"sent_to_print_at\":\"2000-02-12T01:02:03.123Z\","
                                + "\"printed_at\":\"2000-02-12T01:02:03.123Z\","
                                + "\"copies\":{\"Document_1\":1}"
                                + "}"
                ));
    }

    @Test
    void should_return_404_client_error_when_letter_is_not_found_in_database() throws Exception {
        willThrow(LetterNotFoundException.class).given(service).getStatus(letterStatus.id, "false", "false");

        getLetter(letterStatus.id, "false", "false").andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void should_return_409_client_error_when_duplicate_request() throws Exception {
        willThrow(DataIntegrityViolationException.class).given(service).getStatus(letterStatus.id, "false", "true");

        getLetter(letterStatus.id, "false", "true").andExpect(status().is(HttpStatus.CONFLICT.value()));
    }

    @Test
    void should_return_404_client_error_when_invalid_uuid_is_provided() throws Exception {
        getLetter("0987654321", "false", "false").andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        getLetter("X558ff55-37R0-4p6e-80fo-5Lb05b650c44", "false", "false")
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()));

        verify(service, never()).getStatus(any(UUID.class), eq("false"), eq("false"));
    }

    @Test
    void shouldInvokeGetStatusServiceWithDefaultValueWhenIncludeAdditionalInfoIsNull() throws Exception {
        given(service.getStatus(letterStatus.id, "false", "false")).willReturn(letterStatus);

        getLetter(letterStatus.id, null, null)
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "\"id\":\"" + letterStatus.id.toString() + "\","
                                + "\"message_id\":\"" + letterStatus.messageId + "\","
                                + "\"checksum\":\"" + letterStatus.checksum + "\","
                                + "\"created_at\":\"2000-02-12T01:02:03.123Z\","
                                + "\"sent_to_print_at\":\"2000-02-12T01:02:03.123Z\","
                                + "\"printed_at\":\"2000-02-12T01:02:03.123Z\","
                                + "\"copies\":null"
                                + "}"
                ));
    }

    @Test
    void shouldInvokeGetStatusServiceWithNoAdditionalInfoInRequestParam() throws Exception {
        given(service.getStatus(letterStatus.id, "false","false")).willReturn(letterStatus);

        mockMvc.perform(
                get("/letters/" + letterStatus.id)
                .header("ServiceAuthorization", "auth-header-value"))
                .andExpect(status().isOk())
                .andExpect(content().json(
            "{"
                    + "\"id\":\"" + letterStatus.id.toString() + "\","
                    + "\"message_id\":\"" + letterStatus.messageId + "\","
                    + "\"checksum\":\"" + letterStatus.checksum + "\","
                    + "\"created_at\":\"2000-02-12T01:02:03.123Z\","
                    + "\"sent_to_print_at\":\"2000-02-12T01:02:03.123Z\","
                    + "\"printed_at\":\"2000-02-12T01:02:03.123Z\","
                    + "\"copies\":null"
                    + "}"));
    }

    private ResultActions getLetter(String letterId, String isAdditionInfoRequired,
                                    String isDuplicate) throws Exception {
        return mockMvc.perform(
            get("/letters/" + letterId)
                .param("include-additional-info", isAdditionInfoRequired)
                .param("check-duplicate", isDuplicate)
                .header("ServiceAuthorization", "auth-header-value")
        );
    }

    private ResultActions getLetter(UUID letterId, String isAdditionInfoRequired, String isDuplicate) throws Exception {
        return getLetter(letterId.toString(), isAdditionInfoRequired, isDuplicate);
    }
}
