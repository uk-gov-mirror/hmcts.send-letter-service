package uk.gov.hmcts.reform.sendletter.model.out;

import lombok.Data;
import uk.gov.hmcts.reform.sendletter.entity.Report;
import uk.gov.hmcts.reform.sendletter.entity.ReportStatus;

import java.time.LocalDate;

@Data
public class PostedReportTaskResponse {

    final String reportCode;
    final LocalDate reportDate;
    final boolean isInternational;

    long markedPostedCount = 0;

    boolean processingFailed = false;
    String errorMessage = null;

    public void markAsFailed(String errorMessage) {
        this.processingFailed = true;
        this.errorMessage = errorMessage;
    }

    /**
     * Maps a {@link Report} to a {@link PostedReportTaskResponse}, extracting the relevant
     * information and status.
     *
     * @param report the {@link Report}
     * @return the {@link PostedReportTaskResponse} containing the report details and status
     */
    public static PostedReportTaskResponse fromReport(Report report) {
        PostedReportTaskResponse response = new PostedReportTaskResponse(
            report.getReportCode(),
            report.getReportDate(),
            report.isInternational()
        );
        response.setMarkedPostedCount(report.getPrintedLettersCount());
        if (report.getStatus() == ReportStatus.FAIL) {
            response.markAsFailed(report.getErrorMessage());
        }
        return response;
    }
}
