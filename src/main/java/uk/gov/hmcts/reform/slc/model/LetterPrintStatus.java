package uk.gov.hmcts.reform.slc.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public class LetterPrintStatus {

    public final UUID id;
    public final ZonedDateTime printedAt;

    public LetterPrintStatus(UUID id, ZonedDateTime printedAt) {
        this.id = id;
        this.printedAt = printedAt;
    }
}
