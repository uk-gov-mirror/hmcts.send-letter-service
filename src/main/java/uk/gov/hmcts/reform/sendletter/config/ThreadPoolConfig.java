package uk.gov.hmcts.reform.sendletter.config;

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.reform.sendletter.util.TimeZones;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Adds custom error handler to Scheduled Tasks. Followed suggestions by Microsoft.
 * {@see https://github.com/Microsoft/ApplicationInsights-Java/wiki/Distributed-Tracing-in-Asynchronous-Java-Applications#context-propagation-in-scheduled-events}
 */
@Configuration
@EnableScheduling
public class ThreadPoolConfig implements SchedulingConfigurer {

    private static AtomicInteger errorCount = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    private static final Supplier<Long> CURRENT_MILLIS_SUPPLIER = () -> LocalDateTime
        .now()
        .atZone(ZoneId.of(TimeZones.EUROPE_LONDON))
        .toInstant()
        .toEpochMilli();

    private static final Supplier<RequestTelemetryContext> REQUEST_CONTEXT_SUPPLIER = () ->
        new RequestTelemetryContext(CURRENT_MILLIS_SUPPLIER.get(), null);

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler taskScheduler = new SendLetterTaskScheduler();
        taskScheduler.setThreadNamePrefix("SendLetterTask-");
        taskScheduler.setErrorHandler(t -> {
            log.error("Unhandled exception during task. {}: {}", t.getClass(), t.getMessage(), t);
            errorCount.incrementAndGet();
        });
        taskScheduler.initialize();

        taskRegistrar.setTaskScheduler(taskScheduler);
    }

    /**
     * Custom {@link ThreadPoolTaskScheduler} to be able to register scheduled tasks via AppInsights.
     */
    private static class SendLetterTaskScheduler extends ThreadPoolTaskScheduler {

        @Override
        public void execute(Runnable command) {
            super.execute(new WrappedRunnable(command, REQUEST_CONTEXT_SUPPLIER.get()));
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            return super.schedule(new WrappedRunnable(task, REQUEST_CONTEXT_SUPPLIER.get()), trigger);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
            return super.schedule(new WrappedRunnable(task, REQUEST_CONTEXT_SUPPLIER.get()), startTime);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
            return super.scheduleAtFixedRate(new WrappedRunnable(
                task,
                REQUEST_CONTEXT_SUPPLIER.get()
            ), startTime, period);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            return super.scheduleAtFixedRate(new WrappedRunnable(task, REQUEST_CONTEXT_SUPPLIER.get()), period);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
            return super.scheduleWithFixedDelay(new WrappedRunnable(
                task,
                REQUEST_CONTEXT_SUPPLIER.get()
            ), startTime, delay);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
            return super.scheduleWithFixedDelay(new WrappedRunnable(task, REQUEST_CONTEXT_SUPPLIER.get()), delay);
        }
    }

    private static class WrappedRunnable implements Runnable {

        private final Runnable task;
        private RequestTelemetryContext requestContext;

        WrappedRunnable(Runnable task, RequestTelemetryContext requestContext) {
            this.task = task;
            this.requestContext = requestContext;
        }

        @Override
        public void run() {
            if (ThreadContext.getRequestTelemetryContext() != null) {
                ThreadContext.remove();

                // since this runnable is ran on schedule, update the context on every run
                requestContext = REQUEST_CONTEXT_SUPPLIER.get();
            }

            ThreadContext.setRequestTelemetryContext(requestContext);

            task.run();
        }
    }
}

