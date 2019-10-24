package uk.gov.hmcts.reform.sendletter.tasks.reports;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "reports.upload-summary.enabled=false"
})
class DailyLetterUploadSummaryReportDisabledTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void should_not_have_report_sender_in_context() {
        assertThat(context.getBeanNamesForType(DailyLetterUploadSummaryReport.class)).isEmpty();
    }
}
