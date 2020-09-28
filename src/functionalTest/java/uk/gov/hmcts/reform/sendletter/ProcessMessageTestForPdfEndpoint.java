package uk.gov.hmcts.reform.sendletter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sendletter.controllers.MediaTypes;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class ProcessMessageTestForPdfEndpoint extends FunctionalTestSuite {

    @Test
    void should_send_letter_and_upload_file_on_sftp_server_when_letter_contains_one_pdf_document() throws Exception {
        String letterId = sendPrintLetterRequest(
            signIn(),
            samplePdfLetterRequestJson("letter-with-single-pdf.json", "test.pdf")
        );

        awaitAndVerifyFileOnSftp(letterId, (sftpFile, sftp) -> {
            assertThat(sftpFile.getName()).matches(getFileNamePattern(letterId));

            if (!isEncryptionEnabled) {
                validatePdfFile(letterId, sftp, sftpFile, 2);
            }
        });
    }

    @Test
    void should_send_letter_and_upload_file_on_sftp_server_when_letter_contains_two_pdf_document() throws Exception {
        String letterId = sendPrintLetterRequest(
            signIn(),
            samplePdfLetterRequestJson("letter-with-two-pdfs.json", "test.pdf")
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
        return MediaTypes.LETTER_V2;
    }
}
