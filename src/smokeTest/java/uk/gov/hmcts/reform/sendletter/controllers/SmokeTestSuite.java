package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@TestPropertySource("classpath:application.properties")
class SmokeTestSuite {

    @Value("${test-url}")
    String testUrl;

    @Value("${s2s-url}")
    private String s2sUrl;

    @Value("${s2s-name}")
    private String s2sName;

    @Value("${s2s-secret}")
    private String s2sSecret;

    static final String SYNTHETIC_SOURCE_HEADER_VALUE = "Send Letter Service smoke test";

    /**
     * Sign in to s2s.
     *
     * @return s2s JWT token.
     */
    String signIn() {
        Map<String, Object> params = ImmutableMap.of(
            "microservice", this.s2sName,
            "oneTimePassword", new GoogleAuthenticator().getTotpPassword(this.s2sSecret)
        );

        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.s2sUrl)
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .body(params)
            .post("/lease")
            .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response
            .getBody()
            .print();
    }

    String sampleLetterJson(String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }

    RequestSpecification getCommonRequestSpec() {
        return RestAssured
            .given()
            .baseUri(testUrl)
            .relaxedHTTPSValidation()
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, SYNTHETIC_SOURCE_HEADER_VALUE);
    }
}
