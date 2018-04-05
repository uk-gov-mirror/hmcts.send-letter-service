package uk.gov.hmcts.reform.sendletter.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sendletter.exception.TaskRunnerException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Runs tasks in serial using Database locks
 * to ensure the same task is not run concurrently.
 */
public final class SerialTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(SerialTaskRunner.class);
    // Namespace our application locks to avoid potential collisions
    // with other uses of Advisory locks.
    private static final int LOCK_NAMESPACE = 10250;
    private final DataSource source;

    public static SerialTaskRunner get(DataSource source) {
        return new SerialTaskRunner(source);
    }

    private SerialTaskRunner(DataSource source) {
        this.source = source;
    }

    /**
     * Run the task with specified ID if no other task with matching
     * id is already running.
     * No error is thrown if such a task is already running, the
     * supplied Runnable is simply not executed.
     */
    void tryRun(Task task, Runnable runnable) {
        int id = task.getLockId();
        String name = task.name();
        log.info("Trying to lock {}", name);

        try (Connection connection = source.getConnection()) {
            boolean locked = false;
            try {
                if (tryLock(id, connection)) {
                    log.info("Acquired lock {}", name);
                    locked = true;
                    runnable.run();
                } else {
                    log.info("Failed to acquire lock {}", name);
                }
            } finally {
                if (locked) {
                    if (unlock(id, connection)) {
                        log.info("Released lock {}", name);
                    } else {
                        log.warn("Failed to release lock {}", name);
                    }
                }
            }
        } catch (SQLException exc) {
            throw new TaskRunnerException(exc);
        }
    }

    /**
     * Try to acquire a session level advisory lock with specified id without blocking.
     *
     * <p>A session level lock will be held until either explicitly released
     * or the database connection is closed/dies.
     *
     * <p>https://www.postgresql.org/docs/9.4/static/explicit-locking.html#ADVISORY-LOCKS
     * @return true if lock is acquired, false otherwise
     */
    private boolean tryLock(int id, Connection connection) throws SQLException {
        String sql = String.format("SELECT pg_try_advisory_lock(%d, %d);", LOCK_NAMESPACE, id);
        return executeReturningBool(connection, sql);
    }

    private boolean unlock(int id, Connection connection) throws SQLException {
        String sql = String.format("SELECT pg_advisory_unlock(%d, %d);", LOCK_NAMESPACE, id);
        return executeReturningBool(connection, sql);
    }

    /**
     * Execute a SQL statement and interpret the result set
     * as 1 row 1 column single boolean value.
     */
    private boolean executeReturningBool(Connection connection, String sql) throws SQLException {
        try (ResultSet set = connection.createStatement().executeQuery(sql)) {
            if (set.next()) {
                return set.getBoolean(1);
            }
            return false;
        }
    }
}
