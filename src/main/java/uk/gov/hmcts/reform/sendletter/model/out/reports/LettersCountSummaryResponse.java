package uk.gov.hmcts.reform.sendletter.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LettersCountSummaryResponse {

    @JsonProperty("data")
    public final List<LettersCountSummaryItem> items;

    public LettersCountSummaryResponse(List<LettersCountSummaryItem> items) {
        this.items = items;
    }
}
