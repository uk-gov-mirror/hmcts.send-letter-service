package uk.gov.hmcts.reform.sendletter.model;

import java.util.List;

public class ParsedReport {

    public final String path;
    public final List<LetterPrintStatus> statuses;
    public final boolean allRowsParsed;

    public ParsedReport(String path, List<LetterPrintStatus> statuses, boolean allRowsParsed) {
        this.path = path;
        this.statuses = statuses;
        this.allRowsParsed = allRowsParsed;
    }
}
