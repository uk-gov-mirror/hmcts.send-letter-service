package uk.gov.hmcts.reform.slc.services.steps.getpdf.exceptions;

public class PdfMergeException extends RuntimeException {
    public PdfMergeException(String message, Throwable cause) {
        super(message, cause);
    }
}
