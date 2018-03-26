package uk.gov.hmcts.reform.slc.services.steps.sftpupload;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import net.schmizz.sshj.xfer.LocalSourceFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.slc.config.FtpConfigProperties;
import uk.gov.hmcts.reform.slc.logging.AppInsights;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfDoc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FtpClientTest {

    private FtpClient client;

    @Mock private SSHClient sshClient;
    @Mock private SFTPClient sftpClient;
    @Mock private SFTPFileTransfer sftpFileTransfer;
    @Mock private AppInsights insights;
    @Mock private FtpConfigProperties ftpProps;

    @Before
    public void setUp() throws Exception {
        given(sshClient.newSFTPClient()).willReturn(sftpClient);
        given(sftpClient.getFileTransfer()).willReturn(sftpFileTransfer);

        client = new FtpClient(
            () -> sshClient,
            ftpProps,
            insights
        );
    }

    @Test
    public void should_upload_file_to_correct_folder_based_on_whether_its_a_smoke_test_or_not() throws Exception {
        given(ftpProps.getSmokeTestTargetFolder()).willReturn("smoke");
        given(ftpProps.getTargetFolder()).willReturn("regular");

        // when
        client.upload(new PdfDoc("hello.pdf", "hello".getBytes()), true);

        // then
        verify(sftpFileTransfer)
            .upload(
                any(LocalSourceFile.class),
                contains("smoke") //path
            );

        // when
        client.upload(new PdfDoc("hello.pdf", "hello".getBytes()), false);

        // then
        verify(sftpFileTransfer)
            .upload(
                any(LocalSourceFile.class),
                contains("regular") //path
            );
    }
}
