package uk.gov.hmcts.reform.sendletter.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "exception")
public class ExceptionLetter {
    @Id
    private UUID id;
    private String service;
    private LocalDateTime createdAt;
    private String type;
    private String isAsync;
    private String message;

    ExceptionLetter() {
    }

    public ExceptionLetter(UUID id, String service, LocalDateTime createdAt,
                           String type, String message, String isAsync) {
        this.id = id;
        this.service = service;
        this.createdAt = createdAt;
        this.type = type;
        this.message = message;
        this.isAsync = isAsync;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
