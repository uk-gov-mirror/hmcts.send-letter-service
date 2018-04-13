package uk.gov.hmcts.reform.sendletter.services.encryption;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class UnableToPgpEncryptZipFileException extends UnknownErrorCodeException {

    private static final long serialVersionUID = 262685880220521685L;

    public UnableToPgpEncryptZipFileException(Throwable cause) {
        super(AlertLevel.P1, cause);
    }
}
