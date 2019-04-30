package uk.gov.hmcts.reform.sendletter.entity.reports;

public class ServiceLettersCount implements ServiceLettersCountSummary {

    private final String service;

    private final int uploaded;

    public ServiceLettersCount(String service, int uploaded) {
        this.service = service;
        this.uploaded = uploaded;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public int getUploaded() {
        return uploaded;
    }
}
