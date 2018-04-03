package uk.gov.hmcts.reform.sendletter.tasks;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.time.LocalTime;
import javax.sql.DataSource;

@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class TaskSchedule {
    private final MarkLettersPostedTask markPosted;
    private final UploadLettersTask upload;
    private final DataSource dataSource;

    public TaskSchedule(UploadLettersTask upload, MarkLettersPostedTask post, DataSource source) {
        this.upload = upload;
        this.markPosted = post;
        this.dataSource = source;
    }

    @Scheduled(fixedDelayString = "${tasks.upload-letters}")
    public void uploadLetters() throws SQLException {
        tryRun(Task.UploadLetters, upload::run);
    }

    @Scheduled(cron = "${tasks.mark-letters-posted}")
    public void markPosted() throws SQLException {
        tryRun(Task.MarkLettersPosted, () -> markPosted.run(LocalTime.now()));
    }

    private void tryRun(Task task, Runnable runnable) throws SQLException {
        SerialTaskRunner.get(dataSource).tryRun(task.getLockId(), runnable);
    }
}
