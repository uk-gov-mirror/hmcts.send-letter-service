package uk.gov.hmcts.reform.sendletter.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class BasicLetterInfo {

    private UUID id;
    private String checksum;
    private String service;
    private String status;
    private String type;
    private String encryptionKeyFingerprint;
    private LocalDateTime createdAt;
    private LocalDateTime sentToPrintAt;
    private LocalDateTime printedAt;

    public BasicLetterInfo(
        UUID id,
        String checksum,
        String service,
        LetterStatus status,
        String type,
        String encryptionKeyFingerprint,
        LocalDateTime createdAt,
        LocalDateTime sentToPrintAt,
        LocalDateTime printedAt
    ) {
        this.id = id;
        this.checksum = checksum;
        this.service = service;
        this.status = status.name(); // todo: use enum in this class
        this.type = type;
        this.encryptionKeyFingerprint = encryptionKeyFingerprint;
        this.createdAt = createdAt;
        this.sentToPrintAt = sentToPrintAt;
        this.printedAt = printedAt;
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

    public String getEncryptionKeyFingerprint() {
        return encryptionKeyFingerprint;
    }

    public void setEncryptionKeyFingerprint(String encryptionKeyFingerprint) {
        this.encryptionKeyFingerprint = encryptionKeyFingerprint;
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

    public LocalDateTime getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(LocalDateTime printedAt) {
        this.printedAt = printedAt;
    }
}
