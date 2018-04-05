package uk.gov.hmcts.reform.sendletter.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Size;

public class LetterWithPdfsRequest implements Serializable {

    private static final long serialVersionUID = 5528476697055795883L;

    // TODO: validate pdfs are base64 encoded.
    /**
     * Base64 encoded pdfs.
     */
    @ApiModelProperty(value = "List of base64 encoded pdfs to be printed. Maximum allowed is 10", required = true)
    @Size(min = 1, max = 10)
    public final List<String> documents;

    @ApiModelProperty(value = "Type to be used by Xerox to print documents", required = true)
    @NotEmpty
    public final String type;

    @JsonProperty("additional_data")
    public final Map<String, Object> additionalData;

    public LetterWithPdfsRequest(
        @JsonProperty("documents") List<String> documents,
        @JsonProperty("type") String type,
        @JsonProperty("additional_data") Map<String, Object> additionalData
    ) {
        this.documents = documents;
        this.type = type;
        this.additionalData = additionalData;
    }
}
