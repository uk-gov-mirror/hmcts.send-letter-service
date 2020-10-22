package uk.gov.hmcts.reform.sendletter.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);

    @Async(value = "AsyncExecutor")
    public void run(final Runnable runnable, final Runnable infoLogger) {
        try {
            infoLogger.run();
            runnable.run();
        } catch (RuntimeException e) {
            logger.error("Async task error", e);
        }
    }
}

