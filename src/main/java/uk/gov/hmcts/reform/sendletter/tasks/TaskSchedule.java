package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exception.TaskRunnerException;

import java.time.LocalTime;
import javax.sql.DataSource;

@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class TaskSchedule {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedule.class);

    private final MarkLettersPostedTask markPosted;
    private final UploadLettersTask upload;
    private final StaleLettersTask staleReport;
    private final DataSource dataSource;

    public TaskSchedule(
        UploadLettersTask upload,
        MarkLettersPostedTask post,
        StaleLettersTask staleReport,
        DataSource source
    ) {
        this.upload = upload;
        this.markPosted = post;
        this.staleReport = staleReport;
        this.dataSource = source;
    }

    @Scheduled(fixedDelayString = "${tasks.upload-letters-interval-ms}")
    public void uploadLetters() {
        tryRun(Task.UploadLetters, upload::run);
    }

    @Scheduled(cron = "${tasks.mark-letters-posted}")
    public void markPosted() {
        tryRun(Task.MarkLettersPosted, () -> markPosted.run(LocalTime.now()));
    }

    @Scheduled(cron = "${tasks.stale-letters-report}")
    public void staleLetters() {
        tryRun(Task.StaleLetters, staleReport::run);
    }

    private void tryRun(Task task, Runnable runnable) {
        try {
            SerialTaskRunner.get(dataSource).tryRun(task, runnable);
        } catch (TaskRunnerException exception) {
            log.error(
                String.format(
                    "Error occurred during task %s run. Cause message: %s",
                    task.name(),
                    exception.getCause().getMessage()
                ),
                exception
            );
        }
    }
}
