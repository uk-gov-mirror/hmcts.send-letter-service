package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders.SYNTHETIC_TEST_TEST_NAME;

public class UnauthorisedSmokeTest extends SmokeTestSuite {

    private static final String LETTER_ID = UUID.randomUUID().toString();

    private String createLetterBody;

    @Before
    public void setup() throws IOException {

        createLetterBody = Resources.toString(Resources.getResource("letter.json"), Charsets.UTF_8);
    }

    @Test
    public void must_have_authorisation_header_for_letter_status_endpoint() {
        RequestSpecification specification = getCommonRequestSpec()
            .header(SYNTHETIC_TEST_TEST_NAME, getClass().getEnclosingMethod().getName())
            .when();

        specification.get("/letters/" + LETTER_ID).then().statusCode(SC_UNAUTHORIZED);
        specification.body(createLetterBody).post("/letters").then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void should_not_authorise_with_bad_authorisation_token() {
        RequestSpecification specification = getCommonRequestSpec()
            .header(SYNTHETIC_TEST_TEST_NAME, getClass().getEnclosingMethod().getName())
            .header("ServiceAuthorization", "invalid token")
            .when();

        specification.get("/letters/" + LETTER_ID).then().statusCode(SC_UNAUTHORIZED);
        specification.body(createLetterBody).post("/letters").then().statusCode(SC_UNAUTHORIZED);
    }
}
