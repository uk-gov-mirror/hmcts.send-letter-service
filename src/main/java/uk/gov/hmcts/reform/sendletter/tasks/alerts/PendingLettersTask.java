package uk.gov.hmcts.reform.sendletter.tasks.alerts;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.PendingLettersService;

import java.util.List;

import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class PendingLettersTask {
    private static final Logger logger = LoggerFactory.getLogger(PendingLettersTask.class);
    private static final String TASK_NAME = "PendingLetters";
    private final PendingLettersService pendingLettersService;
    private final AppInsights insights;
    private final int lettersBeforeMins;

    public PendingLettersTask(PendingLettersService pendingLettersService, AppInsights insights,
                              @Value("${tasks.pending-letters-report.before-mins}") int lettersBeforeMins) {
        this.pendingLettersService = pendingLettersService;
        this.insights = insights;
        this.lettersBeforeMins = lettersBeforeMins;
    }

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(cron = "${tasks.pending-letters-report.cron}", zone = EUROPE_LONDON)
    public void run() {
        logger.info("Started '{}' task", TASK_NAME);

        List<BasicLetterInfo> letters = pendingLettersService.getPendingLettersCreatedBeforeTime(lettersBeforeMins);
        letters.forEach(insights::trackPendingLetter);
        int count = letters.size();

        logger.info("Completed '{}' task. Letters found: {}", TASK_NAME, count);
    }

}
