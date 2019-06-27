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

    @JsonProperty("key_fingerprint")
    public final String keyFingerprint;

    public PendingLetter(UUID id, String service, LocalDateTime createdAt, String keyFingerprint) {
        this.id = id;
        this.service = service;
        this.createdAt = createdAt;
        this.keyFingerprint = keyFingerprint;
    }
}
