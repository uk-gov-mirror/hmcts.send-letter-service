package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.logging.appinsights.AbstractAppInsights;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class AppInsights extends AbstractAppInsights {

    static final String LETTER_NOT_PRINTED = "LetterNotPrinted";

    static final String LETTER_PRINT_REPORT = "LetterPrintReportReceived";

    static final String LETTER_UPLOAD_FOR_PRINT = "LetterUploadedForPrint";

    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AppInsights(TelemetryClient telemetry) {
        super(telemetry);

    }

    // events

    public void trackStaleLetter(Letter staleLetter) {
        LocalDateTime sentToPrint = LocalDateTime.ofInstant(staleLetter.getSentToPrintAt().toInstant(), ZoneOffset.UTC);
        Map<String, String> properties = new HashMap<>();

        properties.put("letterId", staleLetter.getId().toString());
        properties.put("messageId", staleLetter.getMessageId());
        properties.put("service", staleLetter.getService());
        properties.put("type", staleLetter.getType());
        properties.put("sentToPrintDayOfWeek", sentToPrint.getDayOfWeek().name());
        properties.put("sentToPrintAt", sentToPrint.format(TIME_FORMAT));

        telemetry.trackEvent(LETTER_NOT_PRINTED, properties, null);
    }

    public void trackPrintReportReceived(ParsedReport report) {
        telemetry.trackEvent(
            LETTER_PRINT_REPORT,
            ImmutableMap.of("isReportParsedFully", BooleanUtils.toStringYesNo(report.allRowsParsed)),
            ImmutableMap.of("reportSize", (double) report.statuses.size())
        );
    }

    // metrics

    public void trackUploadedLetters(int lettersUploaded) {
        telemetry.trackMetric(LETTER_UPLOAD_FOR_PRINT, lettersUploaded);
    }
}
