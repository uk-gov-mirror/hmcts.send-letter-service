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
public class ProcessMessageWithDocumentCountTest extends FunctionalTestSuite {
    private static Logger logger = LoggerFactory.getLogger(ProcessMessageTest.class);

    @Test
    void should_send_letter_and_upload_file_on_sftp_server() throws Exception {
        String letterId = sendPrintLetterRequest(
            signIn(),
            samplePdfLetterRequestJson("letter-with-document-count.json", "test.pdf")
        );

        String status = verifyLetterUploaded(letterId);
        assertThat(status).isEqualTo(LetterStatus.Uploaded.name());

        awaitAndVerifyFileOnSftp(letterId, (sftpFile, sftp) -> {
            assertThat(sftpFile.getName()).matches(getFileNamePattern(letterId));

            if (!isEncryptionEnabled) {
                validatePdfFile(letterId, sftp, sftpFile, 22);
            }
        });
    }

    private String verifyLetterUploaded(String letterId) {
        int counter = 1;
        String letterStatus = LetterStatus.Created.name();
        while (!letterStatus.equals(LetterStatus.Uploaded.name())) {
            try {
                Thread.sleep(LETTER_UPLOAD_DELAY);
                logger.info("Retrieving letter id {} and retry count {} ", letterId, counter++);
                letterStatus = getLetterStatus(letterId);
            } catch (AssertionError e) {
                logger.info("Retry error " + e.getMessage());
            } catch (InterruptedException interruptedException) {
                logger.error(interruptedException.getMessage(), interruptedException);
            }
        }
        return letterStatus;
    }

    @Test
    void should_throw_ConflictException()  {
        executeMultiRequest(this::getLetterRequest);
    }

    private String getLetterRequest() {
        String letterId = "none";
        try {
            letterId = sendPrintLetterRequest(
                    signIn(),
                    samplePdfLetterRequestJson("letter-with-document-count_duplicate.json", "test.pdf")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return letterId;
    }


    @Override
    String getContentType() {
        return MediaTypes.LETTER_V3;
    }
}
