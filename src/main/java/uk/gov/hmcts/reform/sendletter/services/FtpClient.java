package uk.gov.hmcts.reform.sendletter.services;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import net.schmizz.sshj.xfer.LocalSourceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.slc.config.FtpConfigProperties;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.InMemoryDownloadedFile;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.Report;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.exceptions.FtpStepException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

@Component
@EnableConfigurationProperties(FtpConfigProperties.class)
public class FtpClient {

    private static final Logger logger = LoggerFactory.getLogger(FtpClient.class);

    @Autowired
    private final AppInsights insights;

    private final FtpConfigProperties configProperties;

    private final Supplier<SSHClient> sshClientSupplier;

    // region constructor
    @Autowired
    public FtpClient(
        Supplier<SSHClient> sshClientSupplier,
        FtpConfigProperties configProperties,
        AppInsights insights
    ) {
        this.sshClientSupplier = sshClientSupplier;
        this.configProperties = configProperties;
        this.insights = insights;
    }
    // endregion

    public void upload(LocalSourceFile file, boolean isSmokeTestFile) {
        Instant start = Instant.now();

        runWith(sftp -> {
            try {
                String folder = isSmokeTestFile
                    ? configProperties.getSmokeTestTargetFolder()
                    : configProperties.getTargetFolder();

                String path = String.join("/", folder, file.getName());
                sftp.getFileTransfer().upload(file, path);
                insights.trackFtpUpload(Duration.between(start, Instant.now()), true);

                return null;

            } catch (IOException exc) {
                insights.trackFtpUpload(Duration.between(start, Instant.now()), false);
                insights.trackException(exc);

                throw new FtpStepException("Unable to upload file.", exc);
            }
        });
    }

    /**
     * Downloads ALL files from reports directory.
     */
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
                            throw new FtpStepException("Unable to download file " + file.getName(), exc);
                        }
                    })
                    .collect(toList());

            } catch (IOException exc) {
                throw new FtpStepException("Error while downloading reports", exc);
            }
        });
    }

    public void deleteReport(String reportPath) {
        runWith(sftp -> {
            try {
                sftp.rm(reportPath);
                return null;
            } catch (Exception exc) {
                throw new FtpStepException("Error while deleting report: " + reportPath, exc);
            }
        });
    }

    public void testConnection() {
        runWith(sftpClient -> null);
    }

    private <T> T runWith(Function<SFTPClient, T> action) {
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

            try (SFTPClient sftp = ssh.newSFTPClient()) {
                return action.apply(sftp);
            }
        } catch (IOException exc) {
            insights.trackException(exc);

            throw new FtpStepException("Unable to upload file.", exc);
        } finally {
            try {
                if (ssh != null) {
                    ssh.disconnect();
                }
            } catch (IOException e) {
                logger.warn("Error closing ssh connection.");
            }
        }
    }

    private boolean isReportFile(RemoteResourceInfo resourceInfo) {
        return resourceInfo.isRegularFile()
            && resourceInfo.getName().toLowerCase(Locale.getDefault()).endsWith(".csv");
    }
}
