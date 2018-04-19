package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.util.FinalPackageFileNameHelper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static java.time.LocalDateTime.now;

@Component
public class UploadLettersTask {
    private static final Logger logger = LoggerFactory.getLogger(UploadLettersTask.class);
    public static final String SMOKE_TEST_LETTER_TYPE = "smoke_test";

    private final LetterRepository repo;
    private final FtpClient ftp;
    private final FtpAvailabilityChecker availabilityChecker;

    public UploadLettersTask(
        LetterRepository repo,
        FtpClient ftp,
        FtpAvailabilityChecker availabilityChecker
    ) {
        this.repo = repo;
        this.ftp = ftp;
        this.availabilityChecker = availabilityChecker;
    }

    public void run() {
        logger.info("Started letter upload job");

        if (!availabilityChecker.isFtpAvailable(now().toLocalTime())) {
            logger.info("Not processing due to FTP downtime window");
            return;
        }

        // Upload the letters in batches.
        // With each batch we mark them Uploaded so they no longer appear in the query.
        List<Letter> letters;
        do {
            letters = repo.findFirst10ByStatus(LetterStatus.Created);
            letters.forEach(this::uploadLetter);
        } while (!letters.isEmpty());
        logger.info("Completed letter upload job");
    }

    private void uploadLetter(Letter letter) {
        uploadToFtp(letter);

        logger.info("Successfully uploaded letter {}", letter.getId());

        // Upload succeeded, mark the letter as Uploaded.
        letter.setStatus(LetterStatus.Uploaded);
        letter.setSentToPrintAt(Timestamp.from(Instant.now()));

        // remove pdf content, as it's no longer needed
        letter.setFileContent(null);

        repo.saveAndFlush(letter);

        logger.info("Marked letter {} as {}", letter.getId(), letter.getStatus());
    }

    private void uploadToFtp(Letter letter) {

        FileToSend file = new FileToSend(
            FinalPackageFileNameHelper.generateName(letter),
            letter.getFileContent()
        );

        logger.debug(
            "Uploading letter id: {}, messageId: {}, file name: {}",
            letter.getId(),
            letter.getMessageId(),
            file.filename
        );

        ftp.upload(file, isSmokeTest(letter));
    }

    private boolean isSmokeTest(Letter letter) {
        return Objects.equals(letter.getType(), SMOKE_TEST_LETTER_TYPE);
    }
}
