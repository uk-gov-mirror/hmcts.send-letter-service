package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import uk.gov.hmcts.reform.sendletter.exception.FtpException;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate(@Value("${file-upoad.retries}") int numberOfRetries,
                                       @Value("${file-upoad.wait-time-in-ms}") long timeToWait) {
        return RetryTemplate.builder()
            .retryOn(FtpException.class)
            .maxAttempts(numberOfRetries)
            .exponentialBackoff(timeToWait, ExponentialBackOffPolicy.DEFAULT_MULTIPLIER,
                ExponentialBackOffPolicy.DEFAULT_MAX_INTERVAL)
            .build();
    }
}
