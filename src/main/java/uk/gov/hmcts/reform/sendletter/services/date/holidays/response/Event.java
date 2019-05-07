package uk.gov.hmcts.reform.sendletter.services.date.holidays.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class Event {

    public final LocalDate date;
    public final String title;

    public Event(
        @JsonProperty("date") LocalDate date,
        @JsonProperty("title") String title
    ) {
        this.date = date;
        this.title = title;
    }
}
