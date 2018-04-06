package uk.gov.hmcts.reform.sendletter.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.exception.UnauthenticatedException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

    private static final String SERVICE_HEADER = "some-header";

    @Mock
    private AuthTokenValidator validator;

    @Mock
    private AppInsights insights;

    private AuthService service;

    @Before
    public void setUp() {
        service = new AuthService(validator, insights);
    }

    @After
    public void tearDown() {
        reset(validator, insights);
    }

    @Test
    public void should_throw_missing_header_exception_when_it_is_null() {
        // when
        Throwable exception = catchThrowable(() -> service.authenticate(null));

        // then
        assertThat(exception)
            .isInstanceOf(UnauthenticatedException.class)
            .hasMessage("Missing ServiceAuthorization header");

        // and
        verify(validator, never()).getServiceName(anyString());
        verify(insights, never()).trackServiceAuthentication(any(Duration.class), anyBoolean());
    }

    @Test
    public void should_track_failure_in_service_dependency_when_invalid_token_received() {
        // given
        willThrow(InvalidTokenException.class).given(validator).getServiceName(anyString());

        // when
        Throwable exception = catchThrowable(() -> service.authenticate(SERVICE_HEADER));

        // then
        assertThat(exception).isInstanceOf(InvalidTokenException.class);

        // and
        verify(insights).trackServiceAuthentication(any(Duration.class), eq(false));
    }

    @Test
    public void should_track_successful_service_dependency_when_valid_token_received() {
        // given
        given(validator.getServiceName(SERVICE_HEADER)).willReturn("some-service");

        // when
        String serviceName = service.authenticate(SERVICE_HEADER);

        // then
        assertThat(serviceName).isEqualTo("some-service");

        // and
        verify(insights).trackServiceAuthentication(any(Duration.class), eq(true));
    }
}
