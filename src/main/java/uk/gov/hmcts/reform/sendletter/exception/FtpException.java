package uk.gov.hmcts.reform.sendletter.exception;

public class FtpException extends RuntimeException {
    public FtpException(String message, Throwable cause) {
        super(message, cause);
    }
}
