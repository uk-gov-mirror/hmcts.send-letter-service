package uk.gov.hmcts.reform.sendletter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sendletter.controllers.MediaTypes;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class ProcessMessageTest extends FunctionalTestSuite {

    @Test
    void should_send_letter_and_upload_file_on_sftp_server_when_letter_contains_one_document_with_even_pages()
        throws Exception {
        String letterId = sendPrintLetterRequest(
            signIn(),
            sampleLetterRequestJson("letter_single_document.json", "two-page-template.html")
        );

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

        awaitAndVerifyFileOnSftp(letterId, (sftpFile, sftp) -> {
            assertThat(sftpFile.getName()).matches(getFileNamePattern(letterId));

            if (!isEncryptionEnabled) {
                validatePdfFile(letterId, sftp, sftpFile, 4);
            }
        });
    }

    @Override
    String getContentType() {
        return MediaTypes.LETTER_V1;
    }
}
