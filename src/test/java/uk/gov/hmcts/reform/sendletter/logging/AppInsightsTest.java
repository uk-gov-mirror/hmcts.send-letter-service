package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintStatus;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sendletter.logging.AppInsights.DATE_TIME_FORMAT;

@ExtendWith(MockitoExtension.class)
class AppInsightsTest {

    private static final String CHECKSUM = "some-checksum";

    private static final String SERVICE_NAME = "some-service-name";

    private static final String TYPE = "some-type";

    @Captor
    private ArgumentCaptor<Map<String, String>> properties;

    @Mock
    private TelemetryClient telemetry;

    private AppInsights insights;

    private final TelemetryContext context = new TelemetryContext();

    @BeforeEach
    void setUp() {
        insights = new AppInsights();
        ReflectionTestUtils.setField(insights, "telemetryClient", telemetry);
        context.setInstrumentationKey("some-key");
        when(telemetry.getContext()).thenReturn(context);
    }

    @AfterEach
    void tearDown() {
        reset(telemetry);
    }

    // events

    @Test
    void should_track_event_of_not_printed_letter() {
        LocalDateTime sentToPrint = now();

        BasicLetterInfo letter = mock(BasicLetterInfo.class);
        when(letter.getId()).thenReturn(UUID.randomUUID());
        when(letter.getChecksum()).thenReturn(CHECKSUM);
        when(letter.getService()).thenReturn(SERVICE_NAME);
        when(letter.getType()).thenReturn(TYPE);
        when(letter.getSentToPrintAt()).thenReturn(sentToPrint);

        insights.trackStaleLetter(letter);

        Map<String, String> expectedProperties = new HashMap<>();
        expectedProperties.put("letterId", letter.getId().toString());
        expectedProperties.put("checksum", CHECKSUM);
        expectedProperties.put("service", SERVICE_NAME);
        expectedProperties.put("type", TYPE);
        expectedProperties.put("sentToPrintDayOfWeek", sentToPrint.getDayOfWeek().name());
        expectedProperties.put("sentToPrintAt", sentToPrint.format(DATE_TIME_FORMAT));

        verify(telemetry).trackEvent(
            eq(AppInsights.LETTER_NOT_PRINTED),
            properties.capture(),
            eq(null)
        );
        assertThat(properties.getValue()).containsAllEntriesOf(expectedProperties);
    }


    @Test
    void should_track_event_of_letter_not_sent_to_print() {
        LocalDateTime pendingLetter = now();

        BasicLetterInfo letter = mock(BasicLetterInfo.class);
        when(letter.getId()).thenReturn(UUID.randomUUID());
        when(letter.getService()).thenReturn(SERVICE_NAME);
        when(letter.getType()).thenReturn(TYPE);
        when(letter.getCreatedAt()).thenReturn(pendingLetter);

        insights.trackPendingLetter(letter);

        Map<String, String> expectedProperties = new HashMap<>();
        expectedProperties.put("letterId", letter.getId().toString());
        expectedProperties.put("service", SERVICE_NAME);
        expectedProperties.put("type", TYPE);
        expectedProperties.put("createdAt", pendingLetter.format(DATE_TIME_FORMAT));
        expectedProperties.put("createdDayOfWeek", pendingLetter.getDayOfWeek().name());

        verify(telemetry).trackEvent(
            eq(AppInsights.PENDING_LETTER),
            properties.capture(),
            eq(null)
        );
        assertThat(properties.getValue()).containsAllEntriesOf(expectedProperties);
    }

    @Test
    void should_track_events_of_letter_being_printed_from_ftp_report() {
        List<LetterPrintStatus> statuses = Collections.singletonList(new LetterPrintStatus(
            UUID.randomUUID(),
            ZonedDateTime.now()
        ));

        ParsedReport fullyParsedReport = new ParsedReport(
            "/path/to/report",
            statuses,
            true,
            ZonedDateTime.now().toLocalDate());

        ParsedReport partiallyParsedReport = new ParsedReport(
            "/path/to/report",
            statuses,
            false,
            ZonedDateTime.now().toLocalDate());

        ParsedReport emptyReport = new ParsedReport(
            "/path/to/report",
            Collections.emptyList(),
            true,
            ZonedDateTime.now().toLocalDate());

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
}
