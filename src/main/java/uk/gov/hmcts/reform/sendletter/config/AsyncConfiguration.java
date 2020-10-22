package uk.gov.hmcts.reform.sendletter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfiguration {
    private Logger logger = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Bean(name = "AsyncExecutor")
    public Executor getExecutor(@Value("${async.threadpool-size}") int threadPoolSize) {
        logger.info("thread pool size {}", threadPoolSize);
        return Executors.newFixedThreadPool(threadPoolSize,
            (Runnable r) -> {
                Thread t = new Thread(r);
                t.setName("AsyncExecutor");
                t.setDaemon(true);
                return t;
            }
        );
    }
}
