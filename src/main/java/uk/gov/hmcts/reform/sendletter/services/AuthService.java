package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.exception.UnauthenticatedException;
import uk.gov.hmcts.reform.sendletter.logging.AppDependency;
import uk.gov.hmcts.reform.sendletter.logging.AppDependencyCommand;
import uk.gov.hmcts.reform.sendletter.logging.Dependency;

@Component
public class AuthService {

    private final AuthTokenValidator authTokenValidator;

    public AuthService(AuthTokenValidator authTokenValidator) {
        this.authTokenValidator = authTokenValidator;
    }

    @Dependency(value = AppDependency.AUTH_SERVICE, command = AppDependencyCommand.AUTH_SERVICE_HEADER)
    public String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnauthenticatedException("Missing ServiceAuthorization header");
        } else {
            return authTokenValidator.getServiceName(authHeader);
        }
    }
}
