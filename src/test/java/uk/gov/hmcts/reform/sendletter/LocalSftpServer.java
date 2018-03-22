package uk.gov.hmcts.reform.sendletter;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.subsystem.SftpSubsystem;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public final class LocalSftpServer implements AutoCloseable {
    private final SshServer sshd;

    public final int port = 46043;

    // This is the working directory of the SFTP server.
    public static final String pdfFolderName = "moj";

    public final File rootFolder;

    // This is the folder where xerox expects pdf uploads.
    public final File pdfFolder;

    public static LocalSftpServer create() throws IOException {
        TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        File root = tmp.getRoot();
        File workingDirectory = new File(root, pdfFolderName);
        workingDirectory.mkdir();
        return new LocalSftpServer(root, workingDirectory);
    }

    private LocalSftpServer(File root, File pdfFolder) throws IOException {
        this.rootFolder = root;
        this.pdfFolder = pdfFolder;
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
