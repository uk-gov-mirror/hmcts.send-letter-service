package uk.gov.hmcts.reform.sendletter.tasks.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.services.DelayedPrintService;
import uk.gov.hmcts.reform.sendletter.services.StaleLetterService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sendletter.tasks.reports.DelayAndStaleReport.EMAIL_SUBJECT;

@ExtendWith(MockitoExtension.class)
class DelayAndStaleReportTest {

    @Mock
    private DelayedPrintService delayedPrintService;

    @Mock
    private StaleLetterService staleLetterService;

    @Mock
    private EmailSender emailSender;

    @Captor
    ArgumentCaptor<Attachment> attachmentArgumentCaptor;

    @Captor
    ArgumentCaptor<String> emailSubjectCaptor;

    @Captor
    ArgumentCaptor<String[]> recipientsCaptor;

    private DelayAndStaleReport delayAndStaleReport;

    String[] recipients = null;


    @BeforeEach
    void setUp() {
        recipients = new String[]{"test@dummy.com", "test_2@dummy.com"};
        delayAndStaleReport = new DelayAndStaleReport(delayedPrintService, staleLetterService, emailSender,
                recipients, 2);
    }

    @Test
    void should_send_emails() throws IOException {
        File deplayedFile = new File("delayed-file");
        given(delayedPrintService.getDelayLettersAttachment(isA(LocalDateTime.class),
                isA(LocalDateTime.class), anyInt())).willReturn(deplayedFile);

        File staleFile = new File("stale-file");
        given(staleLetterService.getWeeklyStaleLetters()).willReturn(staleFile);

        delayAndStaleReport.send();

        verify(delayedPrintService).getDelayLettersAttachment(isA(LocalDateTime.class),
                isA(LocalDateTime.class), anyInt());
        verify(staleLetterService).getWeeklyStaleLetters();



        verify(emailSender).send(emailSubjectCaptor.capture(), recipientsCaptor.capture(),
                attachmentArgumentCaptor.capture());

        assertThat(emailSubjectCaptor.getValue()).isEqualTo(EMAIL_SUBJECT);
        assertThat(recipientsCaptor.getValue()).isEqualTo(recipients);
        assertThat(attachmentArgumentCaptor.getAllValues().size()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("emailRecipients")
    void should_not_invoke_send_emails_for_empty_recipients(final String[] data) throws IOException {
        delayAndStaleReport = new DelayAndStaleReport(delayedPrintService, staleLetterService,
                emailSender, data, 2);

        delayAndStaleReport.send();

        verify(delayedPrintService, never()).getDelayLettersAttachment(isA(LocalDateTime.class),
                isA(LocalDateTime.class), anyInt());
        verify(emailSender, never()).send(eq(EMAIL_SUBJECT), eq(recipients),
                ArgumentMatchers.<Attachment>any());
    }

    static Stream<Arguments> emailRecipients() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of((Object) new String[]{})
        );
    }

    @Test
    void should_invoke_send_emails_for_only_delayed_exception() throws IOException {
        given(delayedPrintService.getDelayLettersAttachment(isA(LocalDateTime.class),
                isA(LocalDateTime.class), anyInt())).willThrow(new RuntimeException("Error occured"));

        delayAndStaleReport.send();

        verify(emailSender).send(emailSubjectCaptor.capture(), recipientsCaptor.capture(),
                attachmentArgumentCaptor.capture());
        assertThat(emailSubjectCaptor.getValue()).isEqualTo(EMAIL_SUBJECT);
        assertThat(recipientsCaptor.getValue()).isEqualTo(recipients);
        assertThat(attachmentArgumentCaptor.getAllValues().size()).isEqualTo(1);
    }

    @Test
    void should_invoke_send_emails_for_only_stale_exception() throws IOException {
        given(staleLetterService.getWeeklyStaleLetters()).willThrow(new RuntimeException("Error occured"));

        delayAndStaleReport.send();

        verify(emailSender).send(emailSubjectCaptor.capture(), recipientsCaptor.capture(),
                attachmentArgumentCaptor.capture());
        assertThat(emailSubjectCaptor.getValue()).isEqualTo(EMAIL_SUBJECT);
        assertThat(recipientsCaptor.getValue()).isEqualTo(recipients);
        assertThat(attachmentArgumentCaptor.getAllValues().size()).isEqualTo(1);
    }

    @Test
    void should_not_invoke_send_emails_for_stale_exception() throws IOException {
        given(delayedPrintService.getDelayLettersAttachment(isA(LocalDateTime.class),
                isA(LocalDateTime.class), anyInt())).willThrow(new RuntimeException("Error occured"));

        given(staleLetterService.getWeeklyStaleLetters()).willThrow(new RuntimeException("Error occured"));

        delayAndStaleReport.send();

        verify(emailSender, never()).send(emailSubjectCaptor.capture(), recipientsCaptor.capture(),
                attachmentArgumentCaptor.capture());
    }
}
