package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sendletter.exception.TaskRunnerException;

import javax.sql.DataSource;

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

    public void uploadLetters() {
        tryRun(Task.UploadLetters, upload::run);
    }

    public void markPosted() {
        tryRun(Task.MarkLettersPosted, markPosted::run);
    }

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
