package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import java.sql.Timestamp;
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

    private String messageId;
    private String service;
    // The following Type annotation instructs jpa to JSON serialize this field.
    // The column annotation instructs jpa that this field is stored as a json column
    // in our database and should be addressed with ::json in SQL fragments.
    @Type(type = "json")
    @Column(columnDefinition = "json")
    private JsonNode additionalData;
    private Timestamp createdAt;
    private Timestamp sentToPrintAt;
    private Timestamp printedAt;
    private boolean isFailed;
    private String type;
    @Enumerated(EnumType.STRING)
    private LetterStatus status = LetterStatus.Created;
    private byte[] fileContent;
    private Boolean isEncrypted;

    // For use by hibernate.
    private Letter() {
    }

    public Letter(
        UUID id,
        String messageId,
        String service,
        JsonNode additionalData,
        String type,
        byte[] fileContent,
        Boolean isEncrypted,
        Timestamp createdAt
    ) {
        this.id = id;
        this.messageId = messageId;
        this.service = service;
        this.additionalData = additionalData;
        this.type = type;
        this.fileContent = fileContent;
        this.isFailed = false;
        this.isEncrypted = isEncrypted;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getSentToPrintAt() {
        return sentToPrintAt;
    }

    public void setSentToPrintAt(Timestamp value) {
        this.sentToPrintAt = value;
    }

    public Timestamp getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(Timestamp value) {
        this.printedAt = value;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public void setAdditionalData(JsonNode additionalData) {
        this.additionalData = additionalData;
    }

    public JsonNode getAdditionalData() {
        return additionalData;
    }

    public Boolean isEncrypted() {
        return isEncrypted;
    }

    public void setIsEncrypted(Boolean encrypted) {
        isEncrypted = encrypted;
    }
}
