package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sendletter.exception.FtpException;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileInfo;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.time.Duration;
import java.util.List;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;

/**
 * Deletes old files from SFTP.
 */
public class DeleteOldFilesTask {

    private static final Logger logger = LoggerFactory.getLogger(DeleteOldFilesTask.class);

    private final FtpClient ftp;
    private final ServiceFolderMapping serviceFolderMapping;
    private final Duration ttl;

    // region constructor
    public DeleteOldFilesTask(
        FtpClient ftp,
        ServiceFolderMapping serviceFolderMapping,
        Duration ttl
    ) {
        this.ftp = ftp;
        this.serviceFolderMapping = serviceFolderMapping;
        this.ttl = ttl;
    }
    // endregion

    public void run() {
        serviceFolderMapping
            .getFolders()
            .forEach(folder -> {
                List<FileInfo> filesToDelete =
                    ftp.listLetters(folder)
                        .stream()
                        .filter(f -> f.modifiedAt.isBefore(now().minus(ttl)))
                        .collect(toList());

                logger.info("Deleting {} old files from {}", filesToDelete.size(), folder);

                filesToDelete.forEach(f -> {
                    try {
                        ftp.deleteFile(f.path);
                    } catch (FtpException exc) {
                        logger.error("Error deleting old file {}", f.path, exc);
                    }
                });
            });
    }
}

