package uk.gov.hmcts.reform.sendletter.exception;

public class PdfMergeException extends RuntimeException {
    public PdfMergeException(String message, Throwable cause) {
        super(message, cause);
    }
}
