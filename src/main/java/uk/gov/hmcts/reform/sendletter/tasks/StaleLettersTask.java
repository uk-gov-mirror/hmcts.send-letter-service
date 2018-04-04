package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.FtpAvailabilityChecker;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Task to run report on unprinted letters and report them to AppInsights.
 */
@Component
public class StaleLettersTask {
    private static final Logger logger = LoggerFactory.getLogger(StaleLettersTask.class);

    private final LetterRepository repo;
    private final AppInsights insights;
    private final LocalTime staleCutOffTime;

    @Autowired
    public StaleLettersTask(
        LetterRepository repo,
        AppInsights insights,
        FtpAvailabilityChecker checker
    ) {
        this.repo = repo;
        this.insights = insights;
        this.staleCutOffTime = checker.getDowntimeStart();
    }

    public void run() {
        Timestamp staleCutOff = Timestamp.valueOf(
            LocalDateTime.now()
                .minusDays(1)
                .with(staleCutOffTime)
        );

        logger.info("Started stale letter report job with cut-off of {}", staleCutOff);

        long staleLetters = repo.findByStatusAndSentToPrintAtBefore(LetterStatus.Uploaded, staleCutOff)
            .peek(insights::trackStaleLetter)
            .count();

        logger.info("Completed stale letter report job. Letters found: {}", staleLetters);
    }
}
