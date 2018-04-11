package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.exception.DocumentZipException;
import uk.gov.hmcts.reform.sendletter.exception.FtpException;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.zip.ZipFileNameHelper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Iterator;
import java.util.Objects;

import static java.time.LocalDateTime.now;

@Component
public class UploadLettersTask {
    private static final Logger logger = LoggerFactory.getLogger(UploadLettersTask.class);

    public static final String SMOKE_TEST_LETTER_TYPE = "smoke_test";

    private final LetterRepository repo;
    private final FtpClient ftp;
    private final FtpAvailabilityChecker availabilityChecker;

    @Autowired
    public UploadLettersTask(
        LetterRepository repo,
        FtpClient ftp,
        FtpAvailabilityChecker availabilityChecker
    ) {
        this.repo = repo;
        this.ftp = ftp;
        this.availabilityChecker = availabilityChecker;
    }

    @Transactional
    public void run() {
        logger.info("Started letter upload job");

        if (!availabilityChecker.isFtpAvailable(now().toLocalTime())) {
            logger.info("Not processing due to FTP downtime window");
            return;
        }

        Iterator<Letter> iterator = repo.findByStatus(LetterStatus.Created).iterator();

        while (iterator.hasNext()) {
            Letter letter = iterator.next();

            try {
                uploadLetter(letter);
            } catch (FtpException exception) {
                logger.error(String.format("Exception uploading letter %s", letter.getId()), exception);

                break;
            } catch (DocumentZipException exception) {
                logger.error(String.format("Failed to zip document for letter %s", letter.getId()), exception);
            }
        }

        logger.info("Completed letter upload job");
    }

    private void uploadLetter(Letter letter) {
        String uploadedFilename = uploadToFtp(letter);

        logger.info(
            "Successfully uploaded letter {}. File name: {}",
            letter.getId(),
            uploadedFilename
        );

        // Upload succeeded, mark the letter as Uploaded.
        letter.setStatus(LetterStatus.Uploaded);
        letter.setSentToPrintAt(Timestamp.from(Instant.now()));

        // remove pdf content, as it's no longer needed
        letter.setFileContent(null);

        repo.saveAndFlush(letter);

        logger.info("Marked letter {} as {}", letter.getId(), letter.getStatus());
    }

    private String uploadToFtp(Letter letter) {

        FileToSend zippedDoc = new FileToSend(
            ZipFileNameHelper.generateName(letter, now()),
            letter.getFileContent()
        );

        logger.debug(
            "Uploading letter id: {}, messageId: {}, zip filename: {}",
            letter.getId(),
            letter.getMessageId(),
            zippedDoc.filename
        );

        ftp.upload(zippedDoc, isSmokeTest(letter));

        return zippedDoc.filename;
    }

    private boolean isSmokeTest(Letter letter) {
        return Objects.equals(letter.getType(), SMOKE_TEST_LETTER_TYPE);
    }
}
