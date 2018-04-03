package uk.gov.hmcts.reform.sendletter.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.logging.appinsights.AbstractAppInsights;

@Component
public class AppInsights extends AbstractAppInsights {

    public AppInsights(TelemetryClient telemetry) {
        super(telemetry);
    }

    public void trackFtpUpload(java.time.Duration duration, boolean success) {
        telemetry.trackDependency(
            AppDependency.FTP_CLIENT,
            AppDependencyCommand.FTP_FILE_UPLOADED,
            new Duration(duration.toMillis()),
            success
        );
    }

    public void trackException(Exception exception) {
        telemetry.trackException(exception);
    }
}
