package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintStatus;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppInsightsTest {

    private static final String MESSAGE_ID = "some-message-id";

    private static final String SERVICE_NAME = "some-service-name";

    private static final String TYPE = "some-type";

    @Captor
    private ArgumentCaptor<Map<String, String>> properties;

    @Mock
    private TelemetryClient telemetry;

    private AppInsights insights;

    private final TelemetryContext context = new TelemetryContext();

    @Before
    public void setUp() {
        context.setInstrumentationKey("some-key");
        when(telemetry.getContext()).thenReturn(context);
        insights = new AppInsights(telemetry);
    }

    @After
    public void tearDown() {
        reset(telemetry);
    }

    // events

    @Test
    public void should_track_event_of_not_printed_letter() {
        Letter letter = new Letter(
            UUID.randomUUID(),
            MESSAGE_ID,
            SERVICE_NAME,
            null,
            TYPE,
            null,
            false,
            Timestamp.valueOf(LocalDateTime.now())
        );

        ZonedDateTime sentToPrint = ZonedDateTime.now(ZoneOffset.UTC);
        letter.setSentToPrintAt(Timestamp.from(sentToPrint.toInstant()));

        insights.trackStaleLetter(letter);

        Map<String, String> expectedProperties = new HashMap<>();
        expectedProperties.put("letterId", letter.getId().toString());
        expectedProperties.put("messageId", MESSAGE_ID);
        expectedProperties.put("service", SERVICE_NAME);
        expectedProperties.put("type", TYPE);
        expectedProperties.put("sentToPrintDayOfWeek", sentToPrint.getDayOfWeek().name());
        expectedProperties.put("sentToPrintAt", sentToPrint.format(AppInsights.TIME_FORMAT));

        verify(telemetry).trackEvent(
            eq(AppInsights.LETTER_NOT_PRINTED),
            properties.capture(),
            eq(null)
        );
        assertThat(properties.getValue()).containsAllEntriesOf(expectedProperties);
    }

    @Test
    public void should_track_events_of_letter_being_printed_from_ftp_report() {
        List<LetterPrintStatus> statuses = Collections.singletonList(new LetterPrintStatus(
            UUID.randomUUID(),
            ZonedDateTime.now()
        ));
        ParsedReport fullyParsedReport = new ParsedReport("/path/to/report", statuses, true);
        ParsedReport partiallyParsedReport = new ParsedReport("/path/to/report", statuses, false);
        ParsedReport emptyReport = new ParsedReport("/path/to/report", Collections.emptyList(), true);

        insights.trackPrintReportReceived(fullyParsedReport);
        insights.trackPrintReportReceived(partiallyParsedReport);
        insights.trackPrintReportReceived(emptyReport);

        verify(telemetry).trackEvent(
            AppInsights.LETTER_PRINT_REPORT,
            ImmutableMap.of("isReportParsedFully", "yes"),
            ImmutableMap.of("reportSize", 1.0)
        );
        verify(telemetry).trackEvent(
            AppInsights.LETTER_PRINT_REPORT,
            ImmutableMap.of("isReportParsedFully", "no"),
            ImmutableMap.of("reportSize", 1.0)
        );
        verify(telemetry).trackEvent(
            AppInsights.LETTER_PRINT_REPORT,
            ImmutableMap.of("isReportParsedFully", "yes"),
            ImmutableMap.of("reportSize", 0.0)
        );
    }

    @Test
    public void should_track_metric_of_letter_amount_sent_to_print() {
        insights.trackUploadedLetters(123);

        verify(telemetry).trackMetric(AppInsights.LETTER_UPLOAD_FOR_PRINT, 123.0);
    }
}
