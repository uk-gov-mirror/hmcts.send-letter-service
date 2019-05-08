package uk.gov.hmcts.reform.sendletter.model.out;

public class LettersCountSummary {

    public final String serviceName;
    public final int uploaded;

    public LettersCountSummary(String serviceName, int uploaded) {
        this.serviceName = serviceName;
        this.uploaded = uploaded;
    }
}
