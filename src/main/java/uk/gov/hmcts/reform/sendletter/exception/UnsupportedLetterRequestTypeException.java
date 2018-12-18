package uk.gov.hmcts.reform.sendletter.exception;

public class UnsupportedLetterRequestTypeException extends RuntimeException {
    public UnsupportedLetterRequestTypeException() {
        super("Unsupported letter request type");
    }
}
