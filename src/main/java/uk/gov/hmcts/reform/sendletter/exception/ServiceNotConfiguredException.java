package uk.gov.hmcts.reform.sendletter.exception;

public class ServiceNotConfiguredException extends RuntimeException {
    public ServiceNotConfiguredException(String message) {
        super(message);
    }
}
