package uk.gov.hmcts.reform.sendletter.services;

import com.google.common.io.Files;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.helper.FtpHelper;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class FtpUploadTest {

    @Test
    void connects_to_ftp() throws Exception {
        try (LocalSftpServer server = LocalSftpServer.create()) {
            FtpHelper.getSuccessfulClient(LocalSftpServer.port).testConnection();
        }
    }

    @Test
    void uploads_file() throws Exception {
        FileToSend doc = new FileToSend("hello.zip", "world".getBytes(), false);
        try (LocalSftpServer server = LocalSftpServer.create()) {
            FtpClient client = FtpHelper.getSuccessfulClient(LocalSftpServer.port);
            client.runWith(sftpClient -> {
                client.upload(doc, LocalSftpServer.SERVICE_FOLDER, sftpClient);
                return null;
            });
            File[] files = server.lettersFolder.listFiles();
            assertThat(files.length).isEqualTo(1);
            String content = new String(Files.toByteArray(files[0]));
            assertThat(content).isEqualTo("world");
        }
    }
}
