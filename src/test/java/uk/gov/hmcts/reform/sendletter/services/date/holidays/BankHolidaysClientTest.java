package uk.gov.hmcts.reform.sendletter.services.date.holidays;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sendletter.services.date.holidays.response.Event;
import uk.gov.hmcts.reform.sendletter.services.date.holidays.response.Holidays;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class BankHolidaysClientTest {

    private BankHolidaysClient client;
    private WireMockServer api;

    @BeforeEach
    public void setUp() {
        this.api = new WireMockServer();
        this.api.start();
        this.client = new BankHolidaysClient(new RestTemplate(), "http://localhost:8080");
    }

    @AfterEach
    void tearDown() {
        this.api.stop();
    }

    @Test
    public void should_fetch_holidays() {
        // given
        api.stubFor(get("/").willReturn(
            okJson("{"
                + "\"division\": \"england-and-wales\","
                + "\"events\": ["
                + "  {"
                + "    \"title\": \"Good Friday\","
                + "    \"date\": \"2012-04-06\","
                + "    \"notes\": \"\","
                + "    \"bunting\": \"false\""
                + "  },"
                + "  {"
                + "    \"title\": \"Easter Monday\","
                + "    \"date\": \"2012-04-09\","
                + "    \"notes\": \"\","
                + "    \"bunting\": \"false\""
                + "  }"
                + "]"
                + "}"
            )));

        // when
        Holidays holidays = client.getHolidays();

        // then
        assertThat(holidays).isNotNull();
        assertThat(holidays.events)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                new Event(LocalDate.of(2012, 4, 6), "Good Friday"),
                new Event(LocalDate.of(2012, 4, 9), "Easter Monday")
            );
    }

    @Test
    public void should_throw_exception_if_error_occurred() {
        // given
        api.stubFor(get("/").willReturn(notFound()));

        // when
        Throwable exc = catchThrowable(client::getHolidays);

        // then
        assertThat(exc).isInstanceOf(RestClientException.class);
    }
}
