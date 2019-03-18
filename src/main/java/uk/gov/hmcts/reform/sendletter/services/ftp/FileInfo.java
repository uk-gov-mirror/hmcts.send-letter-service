package uk.gov.hmcts.reform.sendletter.services.ftp;

import java.time.Instant;

public class FileInfo {

    public final String path;
    public final Instant modifiedAt;

    // region constructor
    public FileInfo(String path, Instant modifiedAt) {
        this.path = path;
        this.modifiedAt = modifiedAt;
    }
    // endregion
}
