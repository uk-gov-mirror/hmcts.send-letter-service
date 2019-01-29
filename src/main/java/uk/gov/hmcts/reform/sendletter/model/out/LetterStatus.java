package uk.gov.hmcts.reform.sendletter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.time.ZonedDateTime;
import java.util.UUID;

public class LetterStatus {

    public final UUID id;

    public final String status;

    @ApiModelProperty(notes = "This field is deprecated, please use `checksum` instead")
    @JsonProperty("message_id")
    public final String messageId;

    @JsonProperty("checksum")
    public final String checksum;

    @JsonProperty("created_at")
    public final ZonedDateTime createdAt;

    @JsonProperty("sent_to_print_at")
    public final ZonedDateTime sentToPrintAt;

    @JsonProperty("printed_at")
    public final ZonedDateTime printedAt;

    @JsonProperty("has_failed")
    public final boolean hasFailed;

    public LetterStatus(
        final UUID id,
        final String status,
        final String checksum,
        final ZonedDateTime createdAt,
        final ZonedDateTime sentToPrintAt,
        final ZonedDateTime printedAt,
        final boolean hasFailed
    ) {
        this.id = id;
        this.status = status;
        this.checksum = checksum;
        this.messageId = checksum;
        this.createdAt = createdAt;
        this.sentToPrintAt = sentToPrintAt;
        this.printedAt = printedAt;
        this.hasFailed = hasFailed;
    }
}
