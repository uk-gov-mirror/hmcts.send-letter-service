package uk.gov.hmcts.reform.sendletter.model.out;

public class MissingReportsResponse {

    public final String serviceName;
    public final boolean isInternational;

    public MissingReportsResponse(String serviceName, boolean isInternational) {
        this.serviceName = serviceName;
        this.isInternational = isInternational;
    }
}
