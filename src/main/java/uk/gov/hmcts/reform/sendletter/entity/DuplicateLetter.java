package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "duplicates")
@TypeDef(name = "json", typeClass = JsonBinaryType.class)
public class DuplicateLetter {
    @Id
    private UUID id;
    private String checksum;
    private String service;
    // The following Type annotation instructs jpa to JSON serialize this field.
    // The column annotation instructs jpa that this field is stored as a json column
    // in our database and should be addressed with ::json in SQL fragments.
    @Type(type = "json")
    @Column(columnDefinition = "json")
    private JsonNode additionalData;
    private LocalDateTime createdAt;
    private String type;
    private byte[] fileContent;
    private Boolean isEncrypted;
    private String encryptionKeyFingerprint;
    @Type(type = "json")
    @Column(columnDefinition = "json")
    private JsonNode copies;
    private String isAsync;

    // For use by hibernate.
    protected DuplicateLetter() {
    }

    public DuplicateLetter(
        UUID id,
        String checksum,
        String service,
        JsonNode additionalData,
        String type,
        byte[] fileContent,
        Boolean isEncrypted,
        String encryptionKeyFingerprint,
        LocalDateTime createdAt,
        JsonNode copies,
        String isAsync
    ) {
        this.id = id;
        this.checksum = checksum;
        this.service = service;
        this.additionalData = additionalData;
        this.createdAt = createdAt;
        this.type = type;
        this.fileContent = fileContent;
        this.isEncrypted = isEncrypted;
        this.encryptionKeyFingerprint = encryptionKeyFingerprint;
        this.copies = copies;
        this.isAsync = isAsync;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public JsonNode getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(JsonNode additionalData) {
        this.additionalData = additionalData;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public Boolean getEncrypted() {
        return isEncrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        isEncrypted = encrypted;
    }

    public String getEncryptionKeyFingerprint() {
        return encryptionKeyFingerprint;
    }

    public void setEncryptionKeyFingerprint(String encryptionKeyFingerprint) {
        this.encryptionKeyFingerprint = encryptionKeyFingerprint;
    }

    public JsonNode getCopies() {
        return copies;
    }

    public void setCopies(JsonNode copies) {
        this.copies = copies;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIsAsync() {
        return isAsync;
    }

    public void setIsAsync(String isAsync) {
        this.isAsync = isAsync;
    }
}
