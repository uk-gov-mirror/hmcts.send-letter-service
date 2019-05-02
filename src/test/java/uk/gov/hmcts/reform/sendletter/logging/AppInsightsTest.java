package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintStatus;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sendletter.logging.AppDependency.FTP_CLIENT;
import static uk.gov.hmcts.reform.sendletter.logging.AppDependencyCommand.FTP_FILE_UPLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.AppDependencyCommand.FTP_REPORT_DELETED;
import static uk.gov.hmcts.reform.sendletter.logging.AppDependencyCommand.FTP_REPORT_DOWNLOADED;
import static uk.gov.hmcts.reform.sendletter.logging.AppInsights.FTP_TYPE;

@ExtendWith(MockitoExtension.class)
class AppInsightsTest {

    private static final String CHECKSUM = "some-checksum";

    private static final String SERVICE_NAME = "some-service-name";

    private static final String TYPE = "some-type";

    @Captor
    private ArgumentCaptor<Map<String, String>> properties;

    @Captor
    private ArgumentCaptor<RemoteDependencyTelemetry> dependencyTelemetryCaptor;

    @Mock
    private TelemetryClient telemetry;

    private AppInsights insights;

    private final TelemetryContext context = new TelemetryContext();

    @BeforeEach
    void setUp() {
        context.setInstrumentationKey("some-key");
        when(telemetry.getContext()).thenReturn(context);
        insights = new AppInsights(telemetry);
    }

    @AfterEach
    void tearDown() {
        reset(telemetry);
    }

    // dependencies

    // doing all dependencies in one go as they are not requiring any logic whatsoever
    @Test
    void should_track_all_ftp_dependencies() {
        Instant now = ZonedDateTime.now().toInstant();
        java.time.Duration duration = java.time.Duration.between(now, now.plusSeconds(1));

        // resetting now to be able to verify exactly what tested
        reset(telemetry);

        insights.trackFtpUpload(duration, true);
        insights.trackFtpReportDeletion(duration, true);
        insights.trackFtpReportDownload(duration, true);

        verify(telemetry, times(3)).trackDependency(dependencyTelemetryCaptor.capture());
        verifyNoMoreInteractions(telemetry);

        reset(telemetry);

        insights.trackFtpUpload(duration, false);
        insights.trackFtpReportDeletion(duration, false);
        insights.trackFtpReportDownload(duration, false);

        verify(telemetry, times(3)).trackDependency(dependencyTelemetryCaptor.capture());
        verifyNoMoreInteractions(telemetry);

        assertThat(dependencyTelemetryCaptor.getAllValues())
            .extracting(dependencyTelemetry -> tuple(
                dependencyTelemetry.getName(),
                dependencyTelemetry.getCommandName(),
                dependencyTelemetry.getSuccess(),
                dependencyTelemetry.getType()
            ))
            .containsOnly(
                tuple(FTP_CLIENT, FTP_FILE_UPLOADED, true, FTP_TYPE),
                tuple(FTP_CLIENT, FTP_REPORT_DELETED, true, FTP_TYPE),
                tuple(FTP_CLIENT, FTP_REPORT_DOWNLOADED, true, FTP_TYPE),
                tuple(FTP_CLIENT, FTP_FILE_UPLOADED, false, FTP_TYPE),
                tuple(FTP_CLIENT, FTP_REPORT_DELETED, false, FTP_TYPE),
                tuple(FTP_CLIENT, FTP_REPORT_DOWNLOADED, false, FTP_TYPE)
            );
    }

    // events

    @Test
    void should_track_event_of_not_printed_letter() {
        Letter letter = new Letter(
            UUID.randomUUID(),
            CHECKSUM,
            SERVICE_NAME,
            null,
            TYPE,
            null,
            false,
            now()
        );

        LocalDateTime sentToPrint = now();
        letter.setSentToPrintAt(sentToPrint);

        insights.trackStaleLetter(letter);

        Map<String, String> expectedProperties = new HashMap<>();
        expectedProperties.put("letterId", letter.getId().toString());
        expectedProperties.put("checksum", CHECKSUM);
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
    void should_track_events_of_letter_being_printed_from_ftp_report() {
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
    void should_track_metric_of_letter_amount_sent_to_print() {
        insights.trackUploadedLetters(123);

        verify(telemetry).trackMetric(AppInsights.LETTER_UPLOAD_FOR_PRINT, 123.0);
    }
}
