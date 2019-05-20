package uk.gov.hmcts.reform.sendletter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public class PendingLetter {

    @JsonProperty("id")
    public final UUID id;

    @JsonProperty("service")
    public final String service;

    @JsonProperty("created_at")
    public final LocalDateTime createdAt;

    public PendingLetter(UUID id, String service, LocalDateTime createdAt) {
        this.id = id;
        this.service = service;
        this.createdAt = createdAt;
    }
}
