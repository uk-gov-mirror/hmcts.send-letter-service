package uk.gov.hmcts.reform.sendletter.e2e;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.controllers.MediaTypes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {"scheduling.enabled=true", "encryption.enabled=true"})
public class EndToEndWithEncryptionEnabledTest extends BaseTest {

    private static final Boolean IS_ENCRYPTION_ENABLED = true;

    @Test
    public void should_handle_old_letter_model() throws Throwable {
        should_upload_letter_and_mark_posted(
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readResource("letter.json")),
            IS_ENCRYPTION_ENABLED
        );
    }

    @Test
    public void should_handle_new_letter_model() throws Throwable {
        should_upload_letter_and_mark_posted(
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaTypes.LETTER_V2)
                .content(readResource("letter-with-pdf.json")),
            IS_ENCRYPTION_ENABLED
        );
    }
}
