package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.persistence.*;

@Entity
@Table(name = "letters")
@TypeDefs({
    @TypeDef(name = "json", typeClass = JsonBinaryType.class)
})
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    public final String messageId;
    public final String service;
    // The following Type annotation instructs jpa to JSON serialize this field.
    // The column annotation instructs jpa that this field is stored as a json column
    // in our database and should be addressed with ::json in SQL fragments.
    @Type(type = "json")
    @Column(columnDefinition = "json")
    public final JsonNode additionalData;
    public final Timestamp createdAt = Timestamp.from(Instant.now());
    public final String type;
    @Enumerated(EnumType.STRING)
    public final LetterState state = LetterState.Created;
    // Base64 encoded PDF.
    public final byte[] pdf;

    protected Letter() {
        messageId = null;
        service = null;
        additionalData = null;
        type = null;
        pdf = null;
    }

    public Letter(
        String messageId,
        String service,
        JsonNode additionalData,
        String type,
        byte[] pdf
    ) {
        this.messageId = messageId;
        this.service = service;
        this.additionalData = additionalData;
        this.type = type;
        this.pdf = pdf;
    }

}
