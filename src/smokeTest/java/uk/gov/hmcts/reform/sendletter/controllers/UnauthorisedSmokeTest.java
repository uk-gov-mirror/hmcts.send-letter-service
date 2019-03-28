package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.UUID;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders.SYNTHETIC_TEST_TEST_NAME;

@ExtendWith(SpringExtension.class)
class UnauthorisedSmokeTest extends SmokeTestSuite {

    private static final String LETTER_ID = UUID.randomUUID().toString();

    private String createLetterBody;

    @BeforeEach
    void setup() throws IOException {

        createLetterBody = Resources.toString(Resources.getResource("letter.json"), Charsets.UTF_8);
    }

    @Test
    void must_have_authorisation_header_for_letter_status_endpoint() {
        RequestSpecification specification = getCommonRequestSpec()
            .header(SYNTHETIC_TEST_TEST_NAME, "must_have_authorisation_header_for_letter_status_endpoint")
            .when();

        specification.get("/letters/" + LETTER_ID).then().statusCode(SC_UNAUTHORIZED);
        specification.body(createLetterBody).post("/letters").then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    void should_not_authorise_with_bad_authorisation_token() {
        RequestSpecification specification = getCommonRequestSpec()
            .header(SYNTHETIC_TEST_TEST_NAME, "should_not_authorise_with_bad_authorisation_token")
            .header("ServiceAuthorization", "invalid token")
            .when();

        specification.get("/letters/" + LETTER_ID).then().statusCode(SC_UNAUTHORIZED);
        specification.body(createLetterBody).post("/letters").then().statusCode(SC_UNAUTHORIZED);
    }
}
