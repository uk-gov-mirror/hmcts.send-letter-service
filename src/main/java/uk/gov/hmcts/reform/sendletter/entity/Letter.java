package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "letters")
@TypeDefs({
    @TypeDef(name = "json", typeClass = JsonBinaryType.class)
})
public class Letter {
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
    private LocalDateTime sentToPrintAt;
    private LocalDateTime printedAt;
    private boolean isFailed;
    private String type;
    @Enumerated(EnumType.STRING)
    private LetterStatus status = LetterStatus.Created;
    private byte[] fileContent;
    private Boolean isEncrypted;
    private String encryptionKeyFingerprint;
    private int copies;

    // For use by hibernate.
    private Letter() {
    }

    public Letter(
        UUID id,
        String checksum,
        String service,
        JsonNode additionalData,
        String type,
        byte[] fileContent,
        Boolean isEncrypted,
        String encryptionKeyFingerprint,
        LocalDateTime createdAt,
        int copies
    ) {
        this.id = id;
        this.checksum = checksum;
        this.service = service;
        this.additionalData = additionalData;
        this.type = type;
        this.fileContent = fileContent;
        this.isFailed = false;
        this.isEncrypted = isEncrypted;
        this.encryptionKeyFingerprint = encryptionKeyFingerprint;
        this.createdAt = createdAt;
        this.copies = copies;
    }

    public UUID getId() {
        return id;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getService() {
        return service;
    }

    public String getType() {
        return type;
    }

    public LetterStatus getStatus() {
        return status;
    }

    public void setStatus(LetterStatus status) {
        this.status = status;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentToPrintAt() {
        return sentToPrintAt;
    }

    public void setSentToPrintAt(LocalDateTime value) {
        this.sentToPrintAt = value;
    }

    public LocalDateTime getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(LocalDateTime value) {
        this.printedAt = value;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public JsonNode getAdditionalData() {
        return additionalData;
    }

    public Boolean isEncrypted() {
        return isEncrypted;
    }

    public String getEncryptionKeyFingerprint() {
        return encryptionKeyFingerprint;
    }

    public void setEncryptionKeyFingerprint(String encryptionKeyFingerprint) {
        this.encryptionKeyFingerprint = encryptionKeyFingerprint;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }
}
