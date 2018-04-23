package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.sendletter.tasks.Task.StaleLetters;

/**
 * Task to run report on unprinted letters and report them to AppInsights.
 */
@Component
public class StaleLettersTask {
    private static final Logger logger = LoggerFactory.getLogger(StaleLettersTask.class);

    private final LetterRepository repo;
    private final AppInsights insights;
    private final LocalTime staleCutOffTime;

    public StaleLettersTask(
        LetterRepository repo,
        AppInsights insights,
        FtpAvailabilityChecker checker
    ) {
        this.repo = repo;
        this.insights = insights;
        this.staleCutOffTime = checker.getDowntimeStart();
    }

    @Transactional
    public void run() {
        Timestamp staleCutOff = Timestamp.valueOf(
            LocalDateTime.now()
                .minusDays(1)
                .with(staleCutOffTime)
        );

        logger.info("Started '{}' task with cut-off of {}", StaleLetters, staleCutOff);

        try (Stream<Letter> letters = repo.findByStatusAndSentToPrintAtBefore(LetterStatus.Uploaded, staleCutOff)) {
            long count = letters.peek(insights::trackStaleLetter).count();
            logger.info("Completed '{}' task. Letters found: {}", StaleLetters, count);
        }
    }
}
