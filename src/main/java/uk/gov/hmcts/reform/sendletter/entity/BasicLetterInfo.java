package uk.gov.hmcts.reform.sendletter.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class BasicLetterInfo {

    private UUID id;
    private String checksum;
    private String service;
    private String status;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime sentToPrintAt;

    public BasicLetterInfo(
        UUID id,
        String checksum,
        String service,
        LetterStatus status,
        String type,
        LocalDateTime createdAt,
        LocalDateTime sentToPrintAt
    ) {
        this.id = id;
        this.checksum = checksum;
        this.service = service;
        this.status = status.name(); // todo: use enum in this class
        this.type = type;
        this.createdAt = createdAt;
        this.sentToPrintAt = sentToPrintAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSentToPrintAt() {
        return sentToPrintAt;
    }

    public void setSentToPrintAt(LocalDateTime sentToPrintAt) {
        this.sentToPrintAt = sentToPrintAt;
    }
}
