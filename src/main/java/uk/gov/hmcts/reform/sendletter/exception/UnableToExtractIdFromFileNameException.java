package uk.gov.hmcts.reform.sendletter.exception;

public class UnableToExtractIdFromFileNameException extends RuntimeException {

    public UnableToExtractIdFromFileNameException(Exception inner) {
        super(inner);
    }

    public UnableToExtractIdFromFileNameException(String message) {
        super(message);
    }
}
