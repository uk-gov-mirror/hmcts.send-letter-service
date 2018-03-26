package uk.gov.hmcts.reform.slc.services.steps.getpdf;

public class UnableToExtractIdFromFileNameException extends RuntimeException {

    public UnableToExtractIdFromFileNameException(Exception inner) {
        super(inner);
    }

    public UnableToExtractIdFromFileNameException(String message) {
        super(message);
    }
}
