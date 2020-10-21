package uk.gov.hmcts.reform.sendletter.services.ftp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RetryOnExceptionStrategy {

    @Value("${file-upoad.retries}")
    public static int DEFAULT_RETRIES;
    @Value("${file-upoad.wait-time-in-milli}")
    public static long DEFAULT_WAIT_TIME_IN_MILLI;

    private int numberOfTriesLeft;
    private long timeToWait;

    public RetryOnExceptionStrategy() {
        this(DEFAULT_RETRIES, DEFAULT_WAIT_TIME_IN_MILLI);
    }

    public RetryOnExceptionStrategy(int numberOfRetries,
                                             long timeToWait) {
        numberOfTriesLeft = numberOfRetries;
        this.timeToWait = timeToWait;
    }

    public boolean shouldRetry() {
        return numberOfTriesLeft > 0;
    }

    public int numberOfTriesLeft() {
        return numberOfTriesLeft;
    }

    public int errorOccured() {
        numberOfTriesLeft--;
        waitUntilNextTry();
        return numberOfTriesLeft;
    }

    public long getTimeToWait() {
        return timeToWait;
    }

    private void waitUntilNextTry() {
        try {
            Thread.sleep(getTimeToWait());
        } catch (InterruptedException ignored) {
            //ignore
        }
    }
}