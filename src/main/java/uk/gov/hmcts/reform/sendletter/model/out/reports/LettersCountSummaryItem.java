package uk.gov.hmcts.reform.sendletter.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LettersCountSummaryItem {

    @JsonProperty("service")
    public final String service;

    @JsonProperty("uploaded")
    public final int uploaded;

    public LettersCountSummaryItem(String service, int uploaded) {
        this.service = service;
        this.uploaded = uploaded;
    }
}
