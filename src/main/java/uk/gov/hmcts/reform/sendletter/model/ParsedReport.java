package uk.gov.hmcts.reform.sendletter.model;

import java.time.LocalDate;
import java.util.List;

public class ParsedReport {

    public final String path;
    public final List<LetterPrintStatus> statuses;
    public final boolean allRowsParsed;
    public final LocalDate reportDate;

    /**
     * Constructor.
     *
     * @param path the path
     * @param statuses the statuses
     * @param allRowsParsed the all rows parsed
     */
    public ParsedReport(String path, List<LetterPrintStatus> statuses, boolean allRowsParsed, LocalDate reportDate) {
        this.path = path;
        this.statuses = statuses;
        this.allRowsParsed = allRowsParsed;
        this.reportDate = reportDate;
    }
}
