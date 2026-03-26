package uk.gov.hmcts.reform.sendletter.services.ftp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import net.schmizz.sshj.userauth.UserAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.support.RetryTemplate;
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
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_CONNECTED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_FILE_DELETED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_FILE_UPLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_LIST_FILES;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_REPORT_DELETED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyCommand.FTP_REPORT_DOWNLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyName.FTP_CLIENT;
import static uk.gov.hmcts.reform.sendletter.logging.DependencyType.FTP;

/**
 * FTP client for uploading and downloading files.
 */
@Component
@EnableConfigurationProperties(FtpConfigProperties.class)
public class FtpClient {

    private static final Logger logger = LoggerFactory.getLogger(FtpClient.class);
    private final FtpConfigProperties configProperties;
    private final Supplier<SSHClient> sshClientSupplier;
    private final RetryTemplate retryTemplate;

    /**
     * Constructor for the FtpClient.
     * @param sshClientSupplier The supplier for SSH client
     * @param configProperties The FTP configuration properties
     * @param retryTemplate The retry template
     */
    public FtpClient(
        Supplier<SSHClient> sshClientSupplier,
        FtpConfigProperties configProperties,
        RetryTemplate retryTemplate
    ) {
        this.sshClientSupplier = sshClientSupplier;
        this.configProperties = configProperties;
        this.retryTemplate = retryTemplate;
    }

    /**
     * Uploads a file to the FTP server.
     * @param file The file to upload
     * @param serviceFolder The folder to upload the file to
     * @param sftpClient The SFTP client
     */
    @Dependency(name = FTP_CLIENT, command = FTP_FILE_UPLOADED, type = FTP)
    public void upload(FileToSend file, String serviceFolder, SFTPClient sftpClient) {
        String folder = file.isSmokeTest
            ? configProperties.getSmokeTestTargetFolder()
            : String.join("/", configProperties.getTargetFolder(), serviceFolder);

        String path = String.join("/", folder, file.getName());
        Instant start = Instant.now();

        retryTemplate.execute(arg -> {
            try {
                logger.debug(
                    "Uploading file {}, size {} to SFTP server",
                    file.filename,
                    file.getLength() / 1024 + "KB"
                );
                sftpClient.getFileTransfer().upload(file, path);

                logger.debug(
                    "File {} uploaded. Time: {}, Size: {}, Folder: {}",
                    file.filename,
                    ChronoUnit.MILLIS.between(start, Instant.now()) + "ms",
                    file.getLength() / 1024 + "KB",
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
                logger.info("Retrying to upload file {} ", file.filename);
                throw new FtpException(String.format("Unable to upload file %s.", file.filename), exc);
            }
            return true;
        });
    }

    /**
     * Downloads reports from the FTP server.
     * @return The list of reports
     */
    @Dependency(name = FTP_CLIENT, command = FTP_REPORT_DOWNLOADED, type = FTP)
    public List<Report> downloadReports() {
        logger.info("Downloading reports from SFTP server");

        return runWith(sftp -> {
            try {
                SFTPFileTransfer transfer = sftp.getFileTransfer();

                List<Report> reports = sftp.ls(configProperties.getReportsFolder())
                    .stream()
                    .filter(this::isReportFile)
                    .map(file -> {
                        InMemoryDownloadedFile inMemoryFile = new InMemoryDownloadedFile();
                        try {
                            transfer.download(file.getPath(), inMemoryFile);
                            Report report = new Report(
                                file.getPath(),
                                inMemoryFile.getBytes(),
                                file.getAttributes().getMtime()
                            );
                            logger.info("Downloaded report file '{}'", report.path);
                            return report;
                        } catch (IOException exc) {
                            throw new FtpException("Unable to download file " + file.getName(), exc);
                        }
                    })
                    .collect(toList());

                logger.info("Finished downloading reports from SFTP server. Number of reports: {}", reports.size());

                return reports;
            } catch (IOException exc) {
                throw new FtpException("Error while downloading reports", exc);
            }
        });
    }

    /**
     * Deletes a report from the FTP server.
     * @param reportPath The path of the report to delete
     */
    @Dependency(name = FTP_CLIENT, command = FTP_REPORT_DELETED, type = FTP)
    public void deleteReport(String reportPath) {
        logger.info("Deleting report '{}'", reportPath);

        runWith(sftp -> {
            try {
                sftp.rm(reportPath);
                logger.info("Deleted report '{}'", reportPath);
                return null;
            } catch (Exception exc) {
                throw new FtpException("Error while deleting report: " + reportPath, exc);
            }
        });
    }

    /**
     * Lists files in a folder on the FTP server.
     * @param serviceFolder The folder to list files from
     * @return The list of files
     */
    @Dependency(name = FTP_CLIENT, command = FTP_LIST_FILES, type = FTP)
    public List<FileInfo> listLetters(String serviceFolder, SFTPClient sftpClient) {
        String path = String.join("/", configProperties.getTargetFolder(), serviceFolder);

        try {
            return sftpClient
                .ls(path)
                .stream()
                .map(it -> new FileInfo(
                    it.getPath(),
                    Instant.ofEpochSecond(it.getAttributes().getMtime())
                ))
                .collect(toList());
        } catch (Exception exc) {
            throw new FtpException("Error while listing files for: " + serviceFolder, exc);
        }
    }

    /**
     * Deletes a file from the FTP server.
     * @param filePath The path of the file to delete
     */
    @Dependency(name = FTP_CLIENT, command = FTP_FILE_DELETED, type = FTP)
    public void deleteFile(String filePath, SFTPClient sftpClient) {
        try {
            logger.info("Deleting file {} on SFTP server", filePath);
            sftpClient.rm(filePath);
            logger.info("Deleted file {} on SFTP server", filePath);
        } catch (Exception exc) {
            throw new FtpException("Error while deleting file: " + filePath, exc);
        }
    }

    /**
     * Runs an action with the SFTP client.
     * @param action The action to run
     * @param <T> The return type of the action
     * @return The result of the action
     */
    public <T> T runWith(Function<SFTPClient, T> action) {
        SSHClient ssh = null;

        try {
            ssh = getSshClient();

            return action.apply(ssh.newSFTPClient());
        } catch (UserAuthException exception) {
            // action not yet applied. schedule will retry on next run
            // logging better message as after 3 retries connection was established
            throw new FtpException("Unable to authenticate.", exception);
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

    /**
     * Gets an SSH client.
     * @return The SSH client
     * @throws IOException If an error occurs while getting the SSH client
     */
    @Dependency(name = FTP_CLIENT, command = FTP_CONNECTED, type = FTP)
    public SSHClient getSshClient() throws IOException {
        SSHClient ssh = sshClientSupplier.get();
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

        return ssh;
    }

    /**
     * Checks if a file is a report file.
     * @param resourceInfo The resource info
     * @return True if the file is a report file, false otherwise
     */
    private boolean isReportFile(RemoteResourceInfo resourceInfo) {
        return resourceInfo.isRegularFile()
            && resourceInfo.getName().toLowerCase(Locale.getDefault()).endsWith(".csv");
    }

}
