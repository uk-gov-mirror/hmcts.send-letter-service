package uk.gov.hmcts.reform.sendletter.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class Report {

    public final String path;
    public final byte[] content;
    public final LocalDate reportDate;

    /**
     * Constructor.
     *
     * @param path the path
     * @param content the content
     * @param mtime the mtime attribute of the downloaded file (epoch second)
     */
    public Report(String path, byte[] content, long mtime) {
        this.path = path;
        this.content = content;
        reportDate = LocalDate.ofInstant(Instant.ofEpochSecond(mtime), ZoneOffset.UTC);
    }
}
