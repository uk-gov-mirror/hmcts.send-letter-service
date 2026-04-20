package uk.gov.hmcts.reform.sendletter;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class CheckReportsFunctionalTest extends FunctionalTestSuite {

    @Test
    void should_check_reports_endpoint() {
        String today = LocalDate.now().toString();

        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(sendLetterServiceUrl)
            .queryParam("startDate", today)
            .queryParam("endDate", today)
            .when()
            .get("/reports/check-reports")
            .andReturn();

        // We can't be sure if reports exist for 'today' in the functional environment,
        // but it should return either 200 or 404.
        assertThat(response.getStatusCode()).isIn(200, 404);

        if (response.getStatusCode() == 404) {
            // If it's 404, verify the response body is a list of missing reports
            assertThat(response.getBody().asString()).contains("service_name");
            assertThat(response.getBody().asString()).contains("is_international");
        }
    }

    @Override
    String getContentType() {
        return "application/json";
    }
}
