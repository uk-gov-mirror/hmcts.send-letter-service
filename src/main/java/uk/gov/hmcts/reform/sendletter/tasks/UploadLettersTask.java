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
import uk.gov.hmcts.reform.sendletter.services.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.zip.ZipFileNameHelper;
import uk.gov.hmcts.reform.sendletter.services.zip.ZippedDoc;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.FileNameHelper;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfDoc;

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
    private final Zipper zipper;
    private final FtpClient ftp;
    private final FtpAvailabilityChecker availabilityChecker;

    @Autowired
    public UploadLettersTask(
        LetterRepository repo,
        Zipper zipper,
        FtpClient ftp,
        FtpAvailabilityChecker availabilityChecker
    ) {
        this.repo = repo;
        this.zipper = zipper;
        this.ftp = ftp;
        this.availabilityChecker = availabilityChecker;
    }

    @Transactional
    public void run() {
        if (!availabilityChecker.isFtpAvailable(now().toLocalTime())) {
            logger.trace("FTP server not available, job cancelled");
            return;
        }

        Iterator<Letter> iterator = repo.findByStatus(LetterStatus.Created).iterator();

        while (iterator.hasNext()) {
            Letter letter = iterator.next();

            try {
                upload(letter);
                logger.debug("Successfully uploaded letter {}", letter.getId());

                // Upload succeeded, mark the letter as Uploaded.
                letter.setStatus(LetterStatus.Uploaded);
                letter.setSentToPrintAt(Timestamp.from(Instant.now()));

                // remove pdf content, as it's no longer needed
                letter.setPdf(null);

                repo.saveAndFlush(letter);

                logger.debug("Marked letter {} as {}", letter.getId(), letter.getStatus());
            } catch (FtpException exception) {
                logger.error(String.format("Exception uploading letter %s", letter.getId()), exception);

                break;
            } catch (DocumentZipException exception) {
                logger.error(String.format("Failed to zip document for letter %s", letter.getId()), exception);
            }
        }
    }

    private void upload(Letter letter) {
        PdfDoc pdfDoc = new PdfDoc(FileNameHelper.generateName(letter, "pdf"), letter.getPdf());
        ZippedDoc zippedDoc = zipper.zip(ZipFileNameHelper.generateName(letter, now()), pdfDoc);

        logger.debug(
            "Uploading letter id: {}, messageId: {}, pdf filename: {}, zip filename:",
            letter.getId(),
            letter.getMessageId(),
            pdfDoc.filename,
            zippedDoc.filename
        );

        ftp.upload(zippedDoc, isSmokeTest(letter));
    }

    private boolean isSmokeTest(Letter letter) {
        return Objects.equals(letter.getType(), SMOKE_TEST_LETTER_TYPE);
    }
}
