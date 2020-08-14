package uk.gov.hmcts.reform.sendletter.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class LetterRequest implements Serializable, ILetterRequest {

    private static final long serialVersionUID = -7737087336283080072L;

    @ApiModelProperty(value = "List of documents to be printed. Maximum allowed is 30", required = true)
    @Size(min = 1, max = 30)
    @Valid
    public final List<Document> documents;

    @ApiModelProperty(value = "Type to be used to print documents", required = true)
    @NotEmpty
    public final String type;

    @ApiModelProperty(value = "Optional field where services can store any additional information about the letter")
    @JsonProperty("additional_data")
    public final Map<String, Object> additionalData;

    public LetterRequest(
        @JsonProperty("documents") List<Document> documents,
        @JsonProperty("type") String type,
        @JsonProperty("additional_data") Map<String, Object> additionalData
    ) {
        this.documents = documents;
        this.type = type;
        this.additionalData = additionalData;
    }


    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public Map<String, Object> getAdditionalData() {
        return this.additionalData;
    }
}
