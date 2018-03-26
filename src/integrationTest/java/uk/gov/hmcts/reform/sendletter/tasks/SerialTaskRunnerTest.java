package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.SQLException;
import javax.sql.DataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class SerialTaskRunnerTest {
    @Autowired DataSource source;

    @Test
    public void runs_task_if_same_task_not_already_running() throws SQLException {
        Runnable runnable = mock(Runnable.class);
        SerialTaskRunner.get(source).tryRun(1, runnable);
        verify(runnable).run();
    }

    @Test
    public void runs_same_task_sequentially() throws SQLException  {
        Runnable runnable = mock(Runnable.class);
        SerialTaskRunner.get(source).tryRun(1, runnable);
        SerialTaskRunner.get(source).tryRun(1, runnable);
        verify(runnable, times(2)).run();
    }

    @Test
    public void does_not_run_same_task_concurrently() throws SQLException {
        Runnable shouldNotRun = mock(Runnable.class);
        tryRun(1, () -> tryRun(1, shouldNotRun));
        verify(shouldNotRun, never()).run();
    }

    @Test
    public void runs_different_tasks_concurrently() throws SQLException {
        Runnable different = mock(Runnable.class);
        tryRun(1, () -> tryRun(2, different));
        verify(different).run();
    }

    // Try to run the task converting any exception to RuntimeException.
    private void tryRun(int taskId, Runnable task) {
        try {
            SerialTaskRunner.get(source).tryRun(taskId, task);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
