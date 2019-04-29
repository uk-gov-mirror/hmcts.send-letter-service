package uk.gov.hmcts.reform.sendletter.model.out;

public class LettersCountSummary {

    public final String service;
    public final int uploaded;

    public LettersCountSummary(String service, int uploaded) {
        this.service = service;
        this.uploaded = uploaded;
    }
}
