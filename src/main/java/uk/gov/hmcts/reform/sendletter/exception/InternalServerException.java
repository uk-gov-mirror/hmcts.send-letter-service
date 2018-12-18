package uk.gov.hmcts.reform.sendletter.exception;

public class InternalServerException extends RuntimeException {
    public InternalServerException(Throwable cause) {
        super(cause);
    }
}
