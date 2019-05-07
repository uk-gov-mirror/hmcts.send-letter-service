package uk.gov.hmcts.reform.sendletter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StaleLetterResponse {

    public final int count;

    @JsonProperty("stale_letters")
    public final List<StaleLetter> staleLetters;

    public StaleLetterResponse(List<StaleLetter> staleLetters) {
        this.staleLetters = staleLetters;
        this.count = staleLetters.size();
    }
}
