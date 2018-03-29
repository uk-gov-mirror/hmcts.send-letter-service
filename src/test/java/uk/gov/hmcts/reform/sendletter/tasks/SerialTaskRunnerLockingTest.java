package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SerialTaskRunnerLockingTest {

    @Mock
    private DataSource source;
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;
    @Mock
    private Runnable task;
    private SerialTaskRunner taskRunner;

    @Before
    public void setup() throws SQLException {
        when(connection.createStatement()).thenReturn(statement);
        when(source.getConnection()).thenReturn(connection);

        taskRunner = SerialTaskRunner.get(source);
    }

    @Test
    public void runs_task_after_successful_locking() throws SQLException {
        setupSuccessfulLocking();
        setupSuccessfulUnlocking();

        taskRunner.tryRun(1, task);

        verify(task, only()).run();
    }

    @Test
    public void runs_task_after_failed_unlocking() throws SQLException {
        setupSuccessfulLocking();
        setupFailedUnlocking();

        taskRunner.tryRun(1, task);

        verify(task, only()).run();
    }

    @Test
    public void does_not_run_task_after_failed_locking() throws SQLException {
        setupFailedLocking();
        setupFailedUnlocking();

        taskRunner.tryRun(1, task);

        verify(task, never()).run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void does_not_run_task_when_obtaining_database_connection_fails() throws SQLException {
        when(source.getConnection()).thenThrow(SQLException.class);

        Throwable thrown = catchThrowable(() -> taskRunner.tryRun(1, task));

        assertThat(thrown).isInstanceOf(SQLException.class);
        verify(task, never()).run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void does_not_run_task_when_locking_throws_exception() throws SQLException {
        when(statement.executeQuery(startsWith("SELECT pg_try_advisory_lock"))).thenThrow(SQLException.class);

        Throwable thrown = catchThrowable(() -> taskRunner.tryRun(1, task));

        assertThat(thrown).isInstanceOf(SQLException.class);
        verify(task, never()).run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void runs_task_when_unlocking_throws_exception() throws SQLException {
        setupSuccessfulLocking();
        when(statement.executeQuery(startsWith("SELECT pg_advisory_unlock"))).thenThrow(SQLException.class);

        Throwable thrown = catchThrowable(() -> taskRunner.tryRun(1, task));

        assertThat(thrown).isInstanceOf(SQLException.class);
        verify(task, only()).run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void throws_exception_threw_by_task() throws SQLException {
        setupSuccessfulLocking();
        setupSuccessfulUnlocking();
        doThrow(RuntimeException.class).when(task).run();

        Throwable thrown = catchThrowable(() -> taskRunner.tryRun(1, task));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(task, only()).run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void throws_exception_threw_by_task_when_unlocking_failed() throws SQLException {
        setupSuccessfulLocking();
        setupFailedUnlocking();
        doThrow(RuntimeException.class).when(task).run();

        Throwable thrown = catchThrowable(() -> taskRunner.tryRun(1, task));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
        verify(task, only()).run();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void throws_exception_when_fails_to_prepare_db_query() throws SQLException {
        when(connection.createStatement()).thenThrow(SQLException.class);
        setupFailedUnlocking();

        Throwable thrown = catchThrowable(() -> taskRunner.tryRun(1, task));

        assertThat(thrown).isInstanceOf(SQLException.class);
        verify(task, never()).run();
    }

    private void setupSuccessfulUnlocking() throws SQLException {
        unlockWith(true);
    }

    private void setupFailedUnlocking() throws SQLException {
        unlockWith(false);
    }

    private void setupSuccessfulLocking() throws SQLException {
        lockWith(true);
    }

    private void setupFailedLocking() throws SQLException {
        lockWith(false);
    }

    private void unlockWith(boolean value) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class); //NOPMD
        when(resultSet.next()).thenReturn(value);
        when(resultSet.getBoolean(1)).thenReturn(value);
        when(statement.executeQuery(startsWith("SELECT pg_advisory_unlock"))).thenReturn(resultSet);
    }

    private void lockWith(boolean value) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class); //NOPMD
        when(resultSet.next()).thenReturn(value);
        when(resultSet.getBoolean(1)).thenReturn(value);
        when(statement.executeQuery(startsWith("SELECT pg_try_advisory_lock"))).thenReturn(resultSet);
    }
}
