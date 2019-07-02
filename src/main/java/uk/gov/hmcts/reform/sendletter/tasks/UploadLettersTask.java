package uk.gov.hmcts.reform.sendletter.tasks;

import net.javacrumbs.shedlock.core.SchedulerLock;
import net.schmizz.sshj.sftp.SFTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.IFtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.util.FinalPackageFileNameHelper;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class UploadLettersTask {
    private static final Logger logger = LoggerFactory.getLogger(UploadLettersTask.class);
    public static final String SMOKE_TEST_LETTER_TYPE = "smoke_test";
    private static final String TASK_NAME = "UploadLetters";

    private final LetterRepository repo;
    private final FtpClient ftp;
    private final IFtpAvailabilityChecker availabilityChecker;
    private final ServiceFolderMapping serviceFolderMapping;
    private final AppInsights insights;

    public UploadLettersTask(
        LetterRepository repo,
        FtpClient ftp,
        IFtpAvailabilityChecker availabilityChecker,
        ServiceFolderMapping serviceFolderMapping,
        AppInsights insights
    ) {
        this.repo = repo;
        this.ftp = ftp;
        this.availabilityChecker = availabilityChecker;
        this.serviceFolderMapping = serviceFolderMapping;
        this.insights = insights;
    }

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(fixedDelayString = "${tasks.upload-letters.interval-ms}")
    public void run() {
        if (!availabilityChecker.isFtpAvailable(now(ZoneId.of(EUROPE_LONDON)).toLocalTime())) {
            logger.info("Not processing '{}' task due to FTP downtime window", TASK_NAME);
            return;
        }

        logger.info("Started '{}' task", TASK_NAME);

        // Upload the letters in batches.
        // With each batch we mark them Uploaded/Skipped so they no longer appear in the query.
        List<Letter> lettersToUpload = repo.findFirst3ByStatus(LetterStatus.Created);
        int counter = 0;

        if (!lettersToUpload.isEmpty()) {
            counter = ftp.runWith(sftpClient -> {
                int uploaded = uploadLetters(lettersToUpload, sftpClient);
                List<Letter> letters;

                do {
                    letters = repo.findFirst3ByStatus(LetterStatus.Created);
                    uploaded += uploadLetters(letters, sftpClient);
                } while (!letters.isEmpty());

                return uploaded;
            });
        }

        if (counter > 0) {
            insights.trackUploadedLetters(counter);
        }

        logger.info("Completed '{}' task. Uploaded {} letters", TASK_NAME, counter);
    }

    private int uploadLetters(List<Letter> lettersToUpload, SFTPClient sftpClient) {
        return lettersToUpload
            .stream()
            .mapToInt(letter -> {
                boolean isUploaded = uploadToFtp(letter, sftpClient);

                if (isUploaded) {
                    letter.setStatus(LetterStatus.Uploaded);
                    letter.setSentToPrintAt(now());
                } else {
                    letter.setStatus(LetterStatus.Skipped);
                }

                updateLetterInDb(letter);

                return isUploaded ? 1 : 0;
            })
            .sum();
    }

    private boolean uploadToFtp(Letter letter, SFTPClient sftpClient) {
        Optional<String> serviceFolder = serviceFolderMapping.getFolderFor(letter.getService());

        if (serviceFolder.isPresent()) {
            FileToSend file = new FileToSend(
                FinalPackageFileNameHelper.generateName(letter),
                letter.getFileContent(),
                isSmokeTest(letter)
            );

            ftp.upload(file, serviceFolder.get(), sftpClient);

            logger.info(
                "Uploaded letter id: {}, checksum: {}, file name: {}, additional data: {}",
                letter.getId(),
                letter.getChecksum(),
                file.filename,
                letter.getAdditionalData()
            );
        } else {
            logger.error("Folder for service {} not found. Skipping letter {}", letter.getService(), letter.getId());
        }

        return serviceFolder.isPresent();
    }

    private void updateLetterInDb(Letter letter) {
        repo.saveAndFlush(letter);

        logger.info("Marked letter {} as {}", letter.getId(), letter.getStatus());
    }

    private boolean isSmokeTest(Letter letter) {
        return Objects.equals(letter.getType(), SMOKE_TEST_LETTER_TYPE);
    }
}
