package uk.gov.hmcts.reform.sendletter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class MissingReportsResponse {

    @JsonProperty("service_name")
    public final String serviceName;

    @JsonProperty("is_international")
    public final boolean isInternational;

    @JsonProperty("report_date")
    public final LocalDate reportDate;

    public MissingReportsResponse(String serviceName, boolean isInternational, LocalDate reportDate) {
        this.serviceName = serviceName;
        this.isInternational = isInternational;
        this.reportDate = reportDate;
    }
}
