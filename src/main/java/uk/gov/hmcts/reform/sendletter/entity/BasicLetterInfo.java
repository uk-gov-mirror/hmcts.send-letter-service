package uk.gov.hmcts.reform.sendletter.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public interface BasicLetterInfo {

    UUID getId();

    String getChecksum();

    String getService();

    String getStatus();

    String getType();

    LocalDateTime getCreatedAt();

    LocalDateTime getSentToPrintAt();
}
