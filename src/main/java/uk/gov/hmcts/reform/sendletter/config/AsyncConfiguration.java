package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfiguration {

    @Bean(name = "AsyncExecutor")
    public Executor getExecutor() {
        return Executors.newFixedThreadPool(10,
            (Runnable r) -> {
                Thread t = new Thread(r);
                t.setName("AsyncExecutor");
                t.setDaemon(true);
                return t;
            }
        );
    }
}
