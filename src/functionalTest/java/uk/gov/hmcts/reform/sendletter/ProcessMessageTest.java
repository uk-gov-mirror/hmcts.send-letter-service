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
class ProcessMessageTest extends FunctionalTestSuite {
    private static Logger logger = LoggerFactory.getLogger(ProcessMessageTest.class);

    @Test
    void should_send_letter_and_upload_file_on_sftp_server_when_letter_contains_one_document_with_even_pages()
        throws Exception {
        String letterId = sendPrintLetterRequest(
            signIn(),
            sampleLetterRequestJson("letter_single_document.json", "two-page-template.html")
        );

        String status = verifyLetterUploaded(letterId);
        assertThat(status).isEqualTo(LetterStatus.Uploaded.name());

        awaitAndVerifyFileOnSftp(letterId, (sftpFile, sftp) -> {
            assertThat(sftpFile.getName()).matches(getFileNamePattern(letterId));

            if (!isEncryptionEnabled) {
                validatePdfFile(letterId, sftp, sftpFile, 2);
            }
        });
    }

    @Test
    void should_send_letter_and_upload_file_on_sftp_server_when_letter_contains_one_document_with_odd_pages()
        throws Exception {
        String letterId = sendPrintLetterRequest(
            signIn(),
            sampleLetterRequestJson("letter_single_document.json", "one-page-template.html")
        );

        String status = verifyLetterUploaded(letterId);
        assertThat(status).isEqualTo(LetterStatus.Uploaded.name());

        awaitAndVerifyFileOnSftp(letterId, (sftpFile, sftp) -> {
            assertThat(sftpFile.getName()).matches(getFileNamePattern(letterId));

            if (!isEncryptionEnabled) {
                validatePdfFile(letterId, sftp, sftpFile, 2);
            }
        });
    }

    @Test
    void should_send_letter_and_upload_file_on_sftp_server_when_letter_contains_two_documents_with_even_pages()
        throws Exception {
        String letterId = sendPrintLetterRequest(
            signIn(),
            sampleLetterRequestJson("letter_two_documents.json", "two-page-template.html")
        );

        String status = verifyLetterUploaded(letterId);
        assertThat(status).isEqualTo(LetterStatus.Uploaded.name());

        awaitAndVerifyFileOnSftp(letterId, (sftpFile, sftp) -> {
            assertThat(sftpFile.getName()).matches(getFileNamePattern(letterId));

            if (!isEncryptionEnabled) {
                validatePdfFile(letterId, sftp, sftpFile, 4);
            }
        });
    }

    @Test
    void should_send_letter_and_upload_file_on_sftp_server_when_letter_contains_two_documents_with_odd_pages()
        throws Exception {
        String letterId = sendPrintLetterRequest(
            signIn(),
            sampleLetterRequestJson("letter_two_documents.json", "one-page-template.html")
        );

        String status = verifyLetterUploaded(letterId);
        assertThat(status).isEqualTo(LetterStatus.Uploaded.name());

        awaitAndVerifyFileOnSftp(letterId, (sftpFile, sftp) -> {
            assertThat(sftpFile.getName()).matches(getFileNamePattern(letterId));

            if (!isEncryptionEnabled) {
                validatePdfFile(letterId, sftp, sftpFile, 4);
            }
        });
    }

    @Test
    void should_throw_ConflictException()  {
        executeMultiRequest(this::getLetterRequest);
    }

    private String getLetterRequest() {
        String letterId = "none";
        try {
            letterId =  sendPrintLetterRequest(
                    signIn(),
                    sampleLetterRequestJson("letter_single_document.json", "one-page-template_duplicate.html")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return letterId;
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

    @Override
    String getContentType() {
        return MediaTypes.LETTER_V1;
    }
}
