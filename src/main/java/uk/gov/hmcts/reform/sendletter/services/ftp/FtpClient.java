package uk.gov.hmcts.reform.sendletter.services.ftp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.FtpConfigProperties;
import uk.gov.hmcts.reform.sendletter.exception.FtpException;
import uk.gov.hmcts.reform.sendletter.logging.Dependency;
import uk.gov.hmcts.reform.sendletter.model.InMemoryDownloadedFile;
import uk.gov.hmcts.reform.sendletter.model.Report;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_FILE_UPLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_REPORT_DELETED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_REPORT_DOWNLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyName.FTP_CLIENT;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyType.FTP;

@Component
@EnableConfigurationProperties(FtpConfigProperties.class)
public class FtpClient {

    private static final Logger logger = LoggerFactory.getLogger(FtpClient.class);

    private final FtpConfigProperties configProperties;

    private final Supplier<SSHClient> sshClientSupplier;

    // region constructor
    public FtpClient(
        Supplier<SSHClient> sshClientSupplier,
        FtpConfigProperties configProperties
    ) {
        this.sshClientSupplier = sshClientSupplier;
        this.configProperties = configProperties;
    }
    // endregion

    @Dependency(name = FTP_CLIENT, command = FTP_FILE_UPLOADED, type = FTP)
    public void upload(FileToSend file, String serviceFolder, SFTPClient sftpClient) {
        String folder = file.isSmokeTest
            ? configProperties.getSmokeTestTargetFolder()
            : String.join("/", configProperties.getTargetFolder(), serviceFolder);

        String path = String.join("/", folder, file.getName());
        Instant start = Instant.now();

        try {
            sftpClient.getFileTransfer().upload(file, path);

            logger.info(
                "File uploaded. Time: {}, Size: {}, Folder: {}",
                ChronoUnit.MILLIS.between(start, Instant.now()) + "ms",
                file.content.length / 1024 + "KB",
                serviceFolder
            );
        } catch (IOException exc) {
            if (exc.getCause() instanceof TimeoutException) {
                logger.error("Timeout error while uploading file. Deleting corrupt file. Path: {}", path, exc);
                // deleting as file is corrupt and will break printing provider
                deleteFile(path, sftpClient);
                // this ^ can also cause FtpException. In case not - we will have the following FtpException
            } else {
                logger.error("Error uploading file. Path: {}", path, exc);
            }

            throw new FtpException("Unable to upload file.", exc);
        }
    }

    /**
     * Downloads ALL files from reports directory.
     */
    @Dependency(name = FTP_CLIENT, command = FTP_REPORT_DOWNLOADED, type = FTP)
    public List<Report> downloadReports() {
        return runWith(sftp -> {
            try {
                SFTPFileTransfer transfer = sftp.getFileTransfer();

                return sftp.ls(configProperties.getReportsFolder())
                    .stream()
                    .filter(this::isReportFile)
                    .map(file -> {
                        InMemoryDownloadedFile inMemoryFile = new InMemoryDownloadedFile();
                        try {
                            transfer.download(file.getPath(), inMemoryFile);
                            return new Report(file.getPath(), inMemoryFile.getBytes());
                        } catch (IOException exc) {
                            throw new FtpException("Unable to download file " + file.getName(), exc);
                        }
                    })
                    .collect(toList());
            } catch (IOException exc) {
                throw new FtpException("Error while downloading reports", exc);
            }
        });
    }

    @Dependency(name = FTP_CLIENT, command = FTP_REPORT_DELETED, type = FTP)
    public void deleteReport(String reportPath) {
        runWith(sftp -> {
            try {
                sftp.rm(reportPath);

                return null;
            } catch (Exception exc) {
                throw new FtpException("Error while deleting report: " + reportPath, exc);
            }
        });
    }

    public List<FileInfo> listLetters(String serviceFolder) {
        return runWith(sftp -> {
            try {
                String path = String.join("/", configProperties.getTargetFolder(), serviceFolder);
                return sftp
                    .ls(path)
                    .stream()
                    .map(it -> new FileInfo(
                            it.getPath(),
                            Instant.ofEpochSecond(it.getAttributes().getMtime())
                        )
                    )
                    .collect(toList());
            } catch (Exception exc) {
                throw new FtpException("Error while listing files for: " + serviceFolder, exc);
            }
        });
    }

    public void deleteFile(String filePath, SFTPClient sftpClient) {
        try {
            sftpClient.rm(filePath);
        } catch (Exception exc) {
            throw new FtpException("Error while deleting file: " + filePath, exc);
        }
    }

    public void testConnection() {
        runWith(sftpClient -> null);
    }

    public <T> T runWith(Function<SFTPClient, T> action) {
        SSHClient ssh = null;

        try {
            ssh = sshClientSupplier.get();

            ssh.addHostKeyVerifier(configProperties.getFingerprint());
            ssh.connect(configProperties.getHostname(), configProperties.getPort());

            ssh.authPublickey(
                configProperties.getUsername(),
                ssh.loadKeys(
                    configProperties.getPrivateKey(),
                    configProperties.getPublicKey(),
                    null
                )
            );

            return action.apply(ssh.newSFTPClient());
        } catch (IOException exc) {
            throw new FtpException("FTP operation failed.", exc);
        } finally {
            try {
                if (ssh != null) {
                    ssh.disconnect();
                }
            } catch (IOException e) {
                logger.warn("Error closing ssh connection.", e);
            }
        }
    }

    private boolean isReportFile(RemoteResourceInfo resourceInfo) {
        return resourceInfo.isRegularFile()
            && resourceInfo.getName().toLowerCase(Locale.getDefault()).endsWith(".csv");
    }

}
