package uk.gov.hmcts.reform.sendletter.tasks.alerts;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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

    /**
     * The function of lockAtLeastFor is to prevent some problems caused by the server time difference
     * and start time difference of the tasks with the same name on each server at the beginning of the task.
     * After this time is set,
     * You can avoid some accidents caused by these small time differences above, and ensure that after a
     * thread grabs the lock, even if it is executed soon, it should not be released immediately, leaving a buffer time.
     * In this way, after multiple threads are started, because the task has been locked, other tasks that have not
     * obtained the lock will not grab the lock again. Note that the time here should not be set for a few seconds and
     * a few minutes, try to be as large as possible
     * lockAtMostFor The function of this setting is to prevent the thread that grabbed the lock, because some
     * accidentally died, and the lock is never released.
     * In this case, although the current execution cycle has failed, if the subsequent execution cycle is not
     * released here, it will never be executed later.
     * Its purpose is not to hide the task, but more importantly, to release the lock and find and solve the problem.
     * As for whether there is a string suffix, there are only two expressions. The number type is milliseconds,
     * and the string type has its own fixed format, for example: PT30S 30s time setting, the unit can be S, M, H
     */
    @SchedulerLock(name = TASK_NAME, lockAtLeastFor = "PT15S", lockAtMostFor = "PT30S")
    @Scheduled(cron = "${tasks.stale-letters-report.cron}", zone = EUROPE_LONDON)
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
