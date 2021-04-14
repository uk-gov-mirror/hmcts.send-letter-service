package uk.gov.hmcts.reform.sendletter.model.out;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

public class PrintJobResponse {
    @ApiModelProperty(
        name = "SAS Token",
        notes = "Shared access token to access blob"
    )
    @JsonProperty("sas_token")
    public final String sasToken;

    @ApiModelProperty(
        name = "Unique ID",
        notes = "Unique id for print request"
    )
    @JsonProperty("request_id")
    public final UUID id;

    public PrintJobResponse(String sasToken, UUID id) {
        this.sasToken = sasToken;
        this.id = id;
    }
}
