package uk.gov.hmcts.reform.sendletter.services.encryption;

public class UnableToLoadPgpPublicKeyException extends RuntimeException {

    private static final long serialVersionUID = -5621910255408117685L;

    public UnableToLoadPgpPublicKeyException(Throwable cause) {
        super(cause);
    }
}
