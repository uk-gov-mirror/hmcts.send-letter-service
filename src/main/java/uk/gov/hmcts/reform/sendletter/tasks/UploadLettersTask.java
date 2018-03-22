package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.FileNameHelper;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfDoc;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.FtpClient;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.exceptions.FtpStepException;

import java.sql.Timestamp;
import java.time.Instant;

public class UploadLettersTask {
    private final LetterRepository repo;
    private final FtpClient ftp;
    private static final Logger logger = LoggerFactory.getLogger(UploadLettersTask.class);

    @Autowired
    public UploadLettersTask(LetterRepository repo, FtpClient ftp) {
        this.repo = repo;
        this.ftp = ftp;
    }

    @Transactional
    public void run() {
        repo.findByState(LetterState.Created).forEach(letter -> {
            try {
                upload(letter);
                logger.debug("Successfully uploaded letter {}", letter.getId());

                // Upload succeeded, mark the letter as Uploaded.
                letter.setState(LetterState.Uploaded);
                letter.setSentToPrintAt(Timestamp.from(Instant.now()));
                repo.saveAndFlush(letter);

                logger.debug("Marked letter {} as Uploaded", letter.getId());
            } catch (FtpStepException e) {
                logger.error("Exception uploading letter {}", letter.getId(), e);
            }
        });
    }

    private void upload(Letter letter) {
        String name = FileNameHelper.generateName(letter, "pdf");
        logger.debug("Uploading letter {}, messageId {}, filename {}",
            letter.getId(), letter.getMessageId(), name);

        ftp.upload(new PdfDoc(name, letter.getPdf()));
    }
}
