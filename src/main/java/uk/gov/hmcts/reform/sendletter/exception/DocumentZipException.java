package uk.gov.hmcts.reform.sendletter.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class DocumentZipException extends UnknownErrorCodeException {

    public DocumentZipException(Throwable cause) {
        super(AlertLevel.P1, cause);
    }
}
