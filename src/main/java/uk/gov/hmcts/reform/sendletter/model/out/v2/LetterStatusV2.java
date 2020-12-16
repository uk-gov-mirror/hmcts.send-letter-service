package uk.gov.hmcts.reform.sendletter.model.out.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

public class LetterStatusV2 extends uk.gov.hmcts.reform.sendletter.model.out.LetterStatus {
    @JsonProperty("copies")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final Map<String, Object> jsonCopies;

    public LetterStatusV2(
        final UUID id,
        final String status,
        final String checksum,
        final ZonedDateTime createdAt,
        final ZonedDateTime sentToPrintAt,
        final ZonedDateTime printedAt,
        final Map<String, Object> additionalData,
        final Map<String, Object> jsonCopies
    ) {
        super(id, status, checksum, createdAt, sentToPrintAt, printedAt, additionalData, null);
        this.jsonCopies = jsonCopies;
    }
}
