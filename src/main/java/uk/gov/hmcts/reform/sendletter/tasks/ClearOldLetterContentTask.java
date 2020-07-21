package uk.gov.hmcts.reform.sendletter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

/**
 * Clears the 'fileContent' of old letters that have not been posted.
 * To be used on AAT only.
 */
@Component
@ConditionalOnProperty(value = "old-letter-content-cleanup.enabled")
public class ClearOldLetterContentTask {

    private static final String TASK_NAME = "ClearOldLetterContent";
    private static final Logger logger = LoggerFactory.getLogger(ClearOldLetterContentTask.class);

    private final LetterRepository letterRepository;
    private final Duration ttl;
    private final Clock clock;

    public ClearOldLetterContentTask(
        LetterRepository letterRepository,
        @Value("${old-letter-content-cleanup.ttl}") Duration ttl,
        Clock clock
    ) {
        this.letterRepository = letterRepository;
        this.ttl = ttl;
        this.clock = clock;
    }

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(cron = "${old-letter-content-cleanup.cron}", zone = EUROPE_LONDON)
    public void run() {
        var cutoff = LocalDateTime.now(clock).minus(ttl);

        logger.info("Starting {} task. Cutoff: {}", TASK_NAME, cutoff);
        int count = letterRepository.clearFileContent(cutoff, LetterStatus.Uploaded);
        logger.info("Completed {} task. Cleared content of {} letters.", TASK_NAME, count);
    }
}
