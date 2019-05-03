package uk.gov.hmcts.reform.sendletter.tasks.reports;

import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.commons.mail.util.MimeMessageParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.sendletter.jupiter.GreenMailExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailSenderTest {

    private static final String TEST_LOGIN = "test@localhost.com";
    private static final String TEST_PASSWORD = "test_password";

    private static final String SUBJECT = "email subject";
    private static final String RECIPIENT_A = "recipient A <a@localhost>";
    private static final String RECIPIENT_B = "b@localhost";
    private static Attachment ATTACHMENT_1;
    private static Attachment ATTACHMENT_2;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @BeforeAll
    static void setUp() throws FileNotFoundException {
        File report = ResourceUtils.getFile("classpath:report.csv");
        ATTACHMENT_1 = new Attachment("filename1", report);
        ATTACHMENT_2 = new Attachment("filename2", report);
    }

    @Test
    void should_send_to_multiple_recipients_without_attachment() throws Exception {
        // given
        String[] recipients = new String[] { RECIPIENT_A, RECIPIENT_B };
        EmailSender emailSender = getEmailSender();

        // when
        emailSender.send(SUBJECT, recipients);

        // then
        MimeMessageParser mimeMessage = new MimeMessageParser(greenMail.getReceivedMessages()[0]).parse();

        assertThat(mimeMessage.getSubject()).isEqualTo(SUBJECT);
        assertThat(mimeMessage.getTo())
            .extracting(Address::toString)
            .containsOnly(RECIPIENT_A, RECIPIENT_B);
        assertThat(mimeMessage.getFrom()).isEqualTo(TEST_LOGIN);
        assertThat(mimeMessage.getPlainContent()).isEqualTo(EmailSender.EMAIL_BODY);
        assertThat(mimeMessage.getAttachmentList()).isEmpty();
    }

    @Test
    void should_send_to_recipient_with_multiple_attachments() throws Exception {
        // given
        EmailSender emailSender = getEmailSender();

        // when
        emailSender.send(SUBJECT, new String[] { RECIPIENT_B }, ATTACHMENT_1, ATTACHMENT_2);

        // then
        MimeMessageParser mimeMessage = new MimeMessageParser(greenMail.getReceivedMessages()[0]).parse();

        assertThat(mimeMessage.getSubject()).isEqualTo(SUBJECT);
        assertThat(mimeMessage.getTo())
            .extracting(Address::toString)
            .containsOnly(RECIPIENT_B);
        assertThat(mimeMessage.getFrom()).isEqualTo(TEST_LOGIN);
        assertThat(mimeMessage.getPlainContent()).isEqualTo(EmailSender.EMAIL_BODY);
        assertThat(mimeMessage.getAttachmentList())
            .hasSize(2)
            .extracting(DataSource::getName)
            .containsOnly(ATTACHMENT_1.filename, ATTACHMENT_2.filename);
    }

    @Test
    void should_not_fail_in_case_sending_threw_an_exception() {
        // given
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailSender emailSender = new EmailSender(mailSender, TEST_LOGIN);
        given(mailSender.createMimeMessage()).willReturn(new JavaMailSenderImpl().createMimeMessage());
        willThrow(new MailException("oh no") {}).given(mailSender).send(any(MimeMessage.class));

        // when
        emailSender.send(SUBJECT, new String[] {});

        // then make verification assuring that previous statement did not throw an error
        verify(mailSender).send(any(MimeMessage.class));
    }

    private EmailSender getEmailSender() {
        greenMail.setUser(TEST_LOGIN, TEST_PASSWORD);

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(greenMail.getSmtp().getServerSetup().getBindAddress());
        mailSender.setPort(ServerSetupTest.SMTP.getPort());
        mailSender.setUsername(TEST_LOGIN);
        mailSender.setPassword(TEST_PASSWORD);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", ServerSetupTest.SMTP.getProtocol());

        return new EmailSender(mailSender, TEST_LOGIN);
    }
}
