package uk.gov.hmcts.reform.sendletter.services.date.holidays.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Holidays {
    public final List<Event> events;

    public Holidays(
        @JsonProperty("events") List<Event> events
    ) {
        this.events = events;
    }
}
