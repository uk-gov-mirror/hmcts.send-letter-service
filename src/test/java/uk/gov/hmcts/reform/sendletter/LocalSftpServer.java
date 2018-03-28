package uk.gov.hmcts.reform.sendletter;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.subsystem.SftpSubsystem;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.Arrays;

public final class LocalSftpServer implements AutoCloseable {
    private final SshServer sshd;

    public static final int port = 46043;

    // These determine where our pdfs and xerox reports are stored.
    public static final String PDF_FOLDER_NAME = "pdfs";
    public static final String REPORT_FOLDER_NAME = "reports";

    public final File rootFolder;

    // This is the folder where xerox expects pdf uploads.
    public final File pdfFolder;

    // Xerox CSV reports are put here.
    public final File reportFolder;

    public static LocalSftpServer create() throws IOException {
        // Fix for intermittent TransportException: Unable to reach a settlement: [] and []
        // I believe this to be caused by TestContainers, on which I raised a bug:
        // https://github.com/testcontainers/testcontainers-java/issues/626
        // We can work around it by removing the problematic shaded SecurityProvider that
        // TestContainers adds and adding one that is not shaded and is in a signed jar.
        BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
        Security.removeProvider(bouncyCastleProvider.getName());
        Security.addProvider(bouncyCastleProvider);
        TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        File root = tmp.getRoot();
        File workingDirectory = new File(root, PDF_FOLDER_NAME);
        workingDirectory.mkdir();
        File reportDirectory = new File(root, "reports");
        reportDirectory.mkdir();
        return new LocalSftpServer(root, workingDirectory, reportDirectory);
    }

    private LocalSftpServer(File root, File pdfFolder, File reportFolder) throws IOException {
        this.rootFolder = root;
        this.pdfFolder = pdfFolder;
        this.reportFolder = reportFolder;
        sshd = SshServer.setUpDefaultServer();

        sshd.setFileSystemFactory(new NativeFileSystemFactory() {
            @Override
            public FileSystemView createFileSystemView(final Session session) {
                return new NativeFileSystemView(session.getUsername(), false) {
                    @Override
                    public String getVirtualUserDir() {
                        return LocalSftpServer.this.rootFolder.getAbsolutePath();
                    }
                };
            }
        });

        sshd.setPort(port);
        sshd.setSubsystemFactories(Arrays.asList(new SftpSubsystem.Factory()));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        // Disable SSL and password checks.
        sshd.setPasswordAuthenticator((a, b, c) -> true);
        sshd.setPublickeyAuthenticator((a, b, c) -> true);

        sshd.start();
    }

    @Override
    public void close() throws Exception {
        sshd.stop();
    }
}
