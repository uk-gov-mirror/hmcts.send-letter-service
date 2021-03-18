package uk.gov.hmcts.reform.sendletter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.schmizz.sshj.sftp.SFTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exception.FtpException;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileInfo;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.IFtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

/**
 * Deletes old files from SFTP.
 */
@Component
@ConditionalOnProperty(value = "file-cleanup.enabled")
public class DeleteOldFilesTask {

    private static final Logger logger = LoggerFactory.getLogger(DeleteOldFilesTask.class);

    private static final String TASK_NAME = "DeleteOldFiles";

    private final FtpClient ftp;
    private final ServiceFolderMapping serviceFolderMapping;
    private final Duration ttl;
    private final IFtpAvailabilityChecker ftpAvailabilityChecker;

    // region constructor
    public DeleteOldFilesTask(
        FtpClient ftp,
        ServiceFolderMapping serviceFolderMapping,
        @Value("${file-cleanup.ttl}") Duration ttl,
        IFtpAvailabilityChecker ftpAvailabilityChecker
    ) {
        this.ftp = ftp;
        this.serviceFolderMapping = serviceFolderMapping;
        this.ttl = ttl;
        this.ftpAvailabilityChecker = ftpAvailabilityChecker;
    }
    // endregion

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(cron = "${file-cleanup.cron}", zone = EUROPE_LONDON)
    public void run() {
        System.out.println("checking git access");
        System.out.println("checking git access");
        System.out.println("checking git access");
        System.out.println("checking git access");
        System.out.println("checking git access");
        System.out.println("checking git access");
        System.out.println("checking git access");
        System.out.println("checking git access");

        if (!ftpAvailabilityChecker.isFtpAvailable(LocalTime.now(ZoneId.of(EUROPE_LONDON)))) {
            logger.info("Not processing '{}' task due to FTP downtime window", TASK_NAME);
            return;
        }
        logger.info("Starting {} task", TASK_NAME);
        serviceFolderMapping
            .getFolders()
            .forEach(folder -> {
                try {
                    // new connection per folder
                    ftp.runWith(sftpClient -> {
                        List<FileInfo> filesToDelete =
                            ftp.listLetters(folder, sftpClient)
                                .stream()
                                .filter(f -> f.modifiedAt.isBefore(now().minus(ttl)))
                                .collect(toList());

                        if (!filesToDelete.isEmpty()) {
                            deleteFiles(folder, filesToDelete, sftpClient);
                        } else {
                            logger.info("No files to delete found");
                        }

                        return null;
                    });
                } catch (Exception e) {
                    logger.error(folder, e);
                }
            });
        logger.info("Completed {} task", TASK_NAME);
    }

    private void deleteFiles(String ftpFolder, List<FileInfo> filesToDelete, SFTPClient sftpClient) {
        logger.info("Deleting {} old files from {}", filesToDelete.size(), ftpFolder);

        filesToDelete.forEach(f -> {
            try {
                ftp.deleteFile(f.path, sftpClient);
            } catch (FtpException exc) {
                logger.error("Error deleting old file {}", f.path, exc);
            }
        });
    }
}

