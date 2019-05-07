package uk.gov.hmcts.reform.sendletter.services.date.holidays;

import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sendletter.services.date.holidays.response.Holidays;

public class BankHolidaysClient {

    private final RestTemplate restTemplate;
    private final String url;

    public BankHolidaysClient(RestTemplate restTemplate, String url) {
        this.restTemplate = restTemplate;
        this.url = url;
    }

    public Holidays getForEngland() {
        return restTemplate.getForObject(url + "/england-and-wales.json", Holidays.class);
    }
}
