package uk.gov.hmcts.reform.sendletter.controllers;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(SpringExtension.class)
class CreateLetterSmokeTest extends SmokeTestSuite {

    static Stream<Arguments> testData() {
        return Stream.of(
                arguments(MediaTypes.LETTER_V2, "letter-with-pdf.json"),
                arguments(MediaTypes.LETTER_V3, "letter-with-document-count.json")
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void shouldCreatLetterSuccessfully(String mediaType, String fileName) throws Exception {

        String jwt = signIn();

        String id = givenJwt(jwt, mediaType)
            .and()
            .body(sampleLetterJson(fileName))
            .when()
            .post("/letters")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .jsonPath()
            .get("letter_id");

        givenJwt(jwt, MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/letters/" + id)
            .then()
            .statusCode(200);
    }

    private RequestSpecification givenJwt(String jwt, String mediaType) {
        return RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, mediaType)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, SYNTHETIC_SOURCE_HEADER_VALUE)
            .header("ServiceAuthorization", "Bearer " + jwt);
    }
}
