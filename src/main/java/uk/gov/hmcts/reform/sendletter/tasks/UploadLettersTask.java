package uk.gov.hmcts.reform.sendletter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.schmizz.sshj.sftp.SFTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.IFtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.util.FinalPackageFileNameHelper;

import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class UploadLettersTask {

    private static final Logger logger = LoggerFactory.getLogger(UploadLettersTask.class);
    public static final int BATCH_SIZE = 10;
    public static final String SMOKE_TEST_LETTER_TYPE = "smoke_test";
    private static final String TASK_NAME = "UploadLetters";

    private final LetterRepository repo;
    private final FtpClient ftp;
    private final IFtpAvailabilityChecker availabilityChecker;
    private final ServiceFolderMapping serviceFolderMapping;

    public UploadLettersTask(
        LetterRepository repo,
        FtpClient ftp,
        IFtpAvailabilityChecker availabilityChecker,
        ServiceFolderMapping serviceFolderMapping
    ) {
        this.repo = repo;
        this.ftp = ftp;
        this.availabilityChecker = availabilityChecker;
        this.serviceFolderMapping = serviceFolderMapping;
    }

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(fixedDelayString = "${tasks.upload-letters.interval-ms}")
    public void run() {
        logger.info("Started '{}' task", TASK_NAME);

        if (!availabilityChecker.isFtpAvailable(now(ZoneId.of(EUROPE_LONDON)).toLocalTime())) {
            logger.info("Not processing '{}' task due to FTP downtime window", TASK_NAME);
        } else {
            if (repo.countByStatus(LetterStatus.Created) > 0) {
                int uploadCount = processLetters();
                logger.info("Completed '{}' task. Uploaded {} letters", TASK_NAME, uploadCount);
            } else {
                logger.info("Completed '{}' task. No letters to upload.", TASK_NAME);
            }
        }
    }

    private int processLetters() {
        return ftp.runWith(client -> {
            int uploadCount = 0;

            for (int i = 0; i < BATCH_SIZE; i++) {
                Optional<Letter> letter = repo.findFirstByStatusOrderByCreatedAtAsc(LetterStatus.Created);
                if (letter.isPresent()) {
                    boolean uploaded = processLetter(letter.get(), client);
                    if (uploaded) {
                        uploadCount++;
                    }
                } else {
                    break;
                }
            }

            return uploadCount;
        });
    }

    private boolean processLetter(Letter letter, SFTPClient sftpClient) {
        Optional<String> serviceFolder = serviceFolderMapping.getFolderFor(letter.getService());

        if (serviceFolder.isPresent()) {
            uploadLetter(letter, serviceFolder.get(), sftpClient);

            letter.setStatus(LetterStatus.Uploaded);
            letter.setSentToPrintAt(now());
            repo.saveAndFlush(letter);

            return true;

        } else {
            logger.error("Folder for service {} not found. Skipping letter {}", letter.getService(), letter.getId());

            letter.setStatus(LetterStatus.Skipped);
            repo.saveAndFlush(letter);

            return false;
        }
    }

    private void uploadLetter(Letter letter, String folder, SFTPClient sftpClient) {
        FileToSend file = new FileToSend(
            FinalPackageFileNameHelper.generateName(letter),
            letter.getFileContent(),
            isSmokeTest(letter)
        );

        ftp.upload(file, folder, sftpClient);

        logger.info(
            "Uploaded letter id: {}, checksum: {}, file name: {}, additional data: {}",
            letter.getId(),
            letter.getChecksum(),
            file.filename,
            letter.getAdditionalData()
        );
    }

    private boolean isSmokeTest(Letter letter) {
        return Objects.equals(letter.getType(), SMOKE_TEST_LETTER_TYPE);
    }
}
