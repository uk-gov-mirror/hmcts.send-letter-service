package uk.gov.hmcts.reform.sendletter.controllers;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

@RunWith(SpringRunner.class)
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class SmokeTestSuite {

    @Value("${test-url:http://localhost:8485}")
    private String testUrl;

    private static final String SYNTHETIC_SOURCE_HEADER_VALUE = "Send Letter Service smoke test";

    RequestSpecification getCommonRequestSpec() {
        return RestAssured.given()
            .baseUri(testUrl)
            .relaxedHTTPSValidation()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, SYNTHETIC_SOURCE_HEADER_VALUE);
    }
}
