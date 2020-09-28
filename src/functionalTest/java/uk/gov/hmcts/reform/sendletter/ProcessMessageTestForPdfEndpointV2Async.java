package uk.gov.hmcts.reform.sendletter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sendletter.controllers.MediaTypes;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class ProcessMessageTestForPdfEndpointV2Async extends FunctionalTestSuite {
    private static Logger logger = LoggerFactory.getLogger(ProcessMessageTestForPdfEndpointV2Async.class);

    @Override
    String getContentType() {
        return MediaTypes.LETTER_V2;
    }

    @Test
    void should_send_letter_and_upload_file_on_sftp_server_when_letter_contains_two_pdf_document() throws Exception {
        String letterId = sendPrintLetterRequestAsync(
            signIn(),
            samplePdfLetterRequestJson("letter-with-ten-pdfs.json", "test.pdf")
        );

        String letterStatus = verifyLetterCreated(letterId);
        assertThat(letterStatus).isEqualTo(LetterStatus.Created.name());
    }

    private String verifyLetterCreated(String letterId) {
        int counter = 1;
        String letterStatus = "Not found";
        while (letterStatus.equals("Not found") && counter <= LETTER_STATUS_RETRY_COUNT) {
            try {
                logger.info("Retrieving letter id {} and retry count {} ", letterId, counter++);
                letterStatus = getLetterStatus(letterId);
            } catch (AssertionError e) {
                try {
                    Thread.sleep(LETTER_STATUS_RETRY_INTERVAL);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }
        return letterStatus;
    }
}
