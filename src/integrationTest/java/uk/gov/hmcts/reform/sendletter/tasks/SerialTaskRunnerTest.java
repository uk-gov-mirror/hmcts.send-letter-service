package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sendletter.tasks.Task.MarkLettersPosted;
import static uk.gov.hmcts.reform.sendletter.tasks.Task.StaleLetters;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class SerialTaskRunnerTest {
    @Autowired DataSource source;

    @Test
    public void runs_task_if_same_task_not_already_running() {
        Runnable runnable = mock(Runnable.class);
        SerialTaskRunner.get(source).tryRun(MarkLettersPosted, runnable);
        verify(runnable).run();
    }

    @Test
    public void runs_same_task_sequentially() {
        Runnable runnable = mock(Runnable.class);
        SerialTaskRunner.get(source).tryRun(MarkLettersPosted, runnable);
        SerialTaskRunner.get(source).tryRun(MarkLettersPosted, runnable);
        verify(runnable, times(2)).run();
    }

    @Test
    public void does_not_run_same_task_concurrently() {
        Runnable shouldNotRun = mock(Runnable.class);
        tryRun(MarkLettersPosted, () -> tryRun(MarkLettersPosted, shouldNotRun));
        verify(shouldNotRun, never()).run();
    }

    @Test
    public void runs_different_tasks_concurrently() {
        Runnable different = mock(Runnable.class);
        tryRun(MarkLettersPosted, () -> tryRun(StaleLetters, different));
        verify(different).run();
    }

    // Try to run the task converting any exception to RuntimeException.
    private void tryRun(Task task, Runnable taskRunner) {
        SerialTaskRunner.get(source).tryRun(task, taskRunner);
    }
}
