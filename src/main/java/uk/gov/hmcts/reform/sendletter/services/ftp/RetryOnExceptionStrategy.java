package uk.gov.hmcts.reform.sendletter.services.ftp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RetryOnExceptionStrategy {

    public static final int DEFAULT_RETRIES = 2;
    public static final long DEFAULT_WAIT_TIME_IN_MILLI = 1000;

    @Value("${file-upoad.retries}")
    public void setNumberOfTriesLeft(int numberOfTriesLeft) {
        this.numberOfTriesLeft = numberOfTriesLeft;
    }

    @Value("${file-upoad.wait-time-in-ms}")
    public void setTimeToWait(long timeToWait) {
        this.timeToWait = timeToWait;
    }

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