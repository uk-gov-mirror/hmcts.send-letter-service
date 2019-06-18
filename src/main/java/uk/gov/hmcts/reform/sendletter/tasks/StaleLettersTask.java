package uk.gov.hmcts.reform.sendletter.tasks;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.StaleLetterService;

import java.util.List;

import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

/**
 * Task to run report on unprinted letters and report them to AppInsights.
 */
@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class StaleLettersTask {
    private static final Logger logger = LoggerFactory.getLogger(StaleLettersTask.class);
    private static final String TASK_NAME = "StaleLetters";

    private final StaleLetterService staleLetterService;
    private final AppInsights insights;

    public StaleLettersTask(
        StaleLetterService staleLetterService,
        AppInsights insights
    ) {
        this.staleLetterService = staleLetterService;
        this.insights = insights;
    }

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(cron = "${tasks.stale-letters-report}", zone = EUROPE_LONDON)
    public void run() {
        logger.info("Started '{}' task", TASK_NAME);

        List<BasicLetterInfo> letters = staleLetterService.getStaleLetters();
        letters.forEach(insights::trackStaleLetter);
        int count = letters.size();

        logger.info("Completed '{}' task. Letters found: {}", TASK_NAME, count);
        if (count > 0) {
            logger.warn("Found {} stale letters", count);
        }
    }
}
