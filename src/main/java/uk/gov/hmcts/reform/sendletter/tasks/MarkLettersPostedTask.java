package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.FtpClient;
import uk.gov.hmcts.reform.slc.model.LetterPrintStatus;
import uk.gov.hmcts.reform.slc.services.ReportParser;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Fetches reports from Xerox SFTP concerning posted
 * letters and sets posted letters as Posted in the database.
 */
public class MarkLettersPostedTask {
    private final LetterRepository repo;
    private final FtpClient ftpClient;
    private final FtpAvailabilityChecker ftpAvailabilityChecker;
    private final ReportParser parser;

    private static final Logger logger = LoggerFactory.getLogger(MarkLettersPostedTask.class);

    @Autowired
    public MarkLettersPostedTask(LetterRepository repo,
                                 FtpClient ftp,
                                 FtpAvailabilityChecker checker,
                                 ReportParser parser) {
        this.repo = repo;
        this.ftpClient = ftp;
        this.ftpAvailabilityChecker = checker;
        this.parser = parser;
    }

    public void run(LocalTime now) {
        logger.trace("Running job");

        if (ftpAvailabilityChecker.isFtpAvailable(now)) {
            ftpClient
                .downloadReports()
                .stream()
                .map(parser::parse)
                .forEach(parsedReport -> {
                    parsedReport.statuses.forEach(this::updatePrintedAt);
                    ftpClient.deleteReport(parsedReport.path);
                });
        } else {
            logger.trace("FTP server not available, job cancelled");
        }
    }

    private void updatePrintedAt(LetterPrintStatus letterPrintStatus) {
        Optional<Letter> optional = repo.findById(letterPrintStatus.id);
        if (optional.isPresent()) {
            Letter letter = optional.get();
            if (letter.getStatus() == LetterStatus.Uploaded) {
                letter.setPrintedAt(Timestamp.from(letterPrintStatus.printedAt.toInstant()));
                letter.setStatus(LetterStatus.Posted);
                repo.save(letter);
                logger.info("Marking letter {} as Posted", letter.getId());
            } else {
                logger.info("Skipping processing of letter {} in state {}", letter.getId(), letter.getStatus());
            }
        } else {
            logger.error("Unknown letter {}", letterPrintStatus.id);
        }
    }
}
