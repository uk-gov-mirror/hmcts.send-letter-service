package uk.gov.hmcts.reform.sendletter.services;

import com.google.common.io.Files;
import org.junit.Test;
import uk.gov.hmcts.reform.sendletter.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.helper.FtpHelper;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfDoc;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class FtpUploadTest {

    @Test
    public void connects_to_ftp() throws Exception {
        try (LocalSftpServer server = LocalSftpServer.create()) {
            FtpHelper.getClient(server.port).testConnection();
        }
    }

    @Test
    public void uploads_file() throws Exception {
        PdfDoc doc = new PdfDoc("hello.txt", "world".getBytes());
        try (LocalSftpServer server = LocalSftpServer.create()) {
            FtpClient client = FtpHelper.getClient(server.port);
            client.upload(doc, false);
            File[] files = server.pdfFolder.listFiles();
            assertThat(files.length).isEqualTo(1);
            String content = new String(Files.toByteArray(files[0]));
            assertThat(content).isEqualTo("world");
        }
    }
}
