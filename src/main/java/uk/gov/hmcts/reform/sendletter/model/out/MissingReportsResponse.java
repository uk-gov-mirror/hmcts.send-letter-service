package uk.gov.hmcts.reform.sendletter.model.out;

public class MissingReportsResponse {

    public final String serviceName;
    public final String type;

    /**
     * Constructor.
     *
     * @param serviceName the service name
     * @param type the type of report
     */
    public MissingReportsResponse(String serviceName, String type) {
        this.serviceName = serviceName;
        this.type = type;
    }
}
