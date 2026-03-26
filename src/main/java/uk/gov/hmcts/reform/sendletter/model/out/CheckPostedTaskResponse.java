package uk.gov.hmcts.reform.sendletter.model.out;

import lombok.Data;

@Data
public class CheckPostedTaskResponse {
    final int markedNoReportAbortedCount;
}
