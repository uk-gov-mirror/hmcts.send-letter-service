package uk.gov.hmcts.reform.sendletter.helper;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import net.schmizz.sshj.SSHClient;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sendletter.LocalSftpServer;
import uk.gov.hmcts.reform.slc.config.FtpConfigProperties;
import uk.gov.hmcts.reform.slc.logging.AppInsights;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.FtpClient;

import java.io.IOException;
import java.util.function.Supplier;

public final class FtpHelper {

    // Prevent instantiation.
    private FtpHelper() {
    }

    // Instantiate an FtpClient with host key verification disabled,
    // so it will connect to a local ftp server without verifying the
    // server's public key.
    public static FtpClient getClient(int port) throws IOException {
        AppInsights insights = Mockito.mock(AppInsights.class);
        Supplier<SSHClient> s = () -> {
            SSHClient client = new SSHClient();
            client.addHostKeyVerifier((a, b, c) -> true);
            return client;
        };
        return new FtpClient(s, getFtpConfig(port), insights);
    }

    private static FtpConfigProperties getFtpConfig(int port) throws IOException {
        FtpConfigProperties p = new FtpConfigProperties();
        p.setHostname("localhost");
        p.setPort(port);
        p.setPublicKey(Resources.toString(Resources.getResource("keypair.pub"), Charsets.UTF_8));
        p.setPrivateKey(Resources.toString(Resources.getResource("keypair"), Charsets.UTF_8));
        p.setUsername("irrelevant");
        p.setFingerprint("SHA1:2Fo8c/96zv32xc8GZWbOGYOlRak=");
        p.setTargetFolder(LocalSftpServer.pdfFolderName);
        return p;
    }
}
