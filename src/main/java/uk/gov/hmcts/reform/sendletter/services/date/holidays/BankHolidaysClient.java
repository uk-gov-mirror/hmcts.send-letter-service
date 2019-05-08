package uk.gov.hmcts.reform.sendletter.services.date.holidays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.sendletter.services.date.holidays.response.Holidays;

@Component
public class BankHolidaysClient {

    private static final String DEFAULT_URL = "https://www.gov.uk/bank-holidays/england-and-wales.json";

    private final RestTemplate restTemplate;
    private final String url;

    @Autowired
    public BankHolidaysClient(RestTemplate restTemplate) {
        this(restTemplate, DEFAULT_URL);
    }

    public BankHolidaysClient(RestTemplate restTemplate, String url) {
        this.restTemplate = restTemplate;
        this.url = url;
    }

    public Holidays getHolidays() {
        return restTemplate.getForObject(url, Holidays.class);
    }
}
