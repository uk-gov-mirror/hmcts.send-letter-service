package uk.gov.hmcts.reform.sendletter.tasks.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    static final String EMAIL_BODY = "This is an auto generated email. Do not respond to it.";

    private final JavaMailSender mailSender;
    private final String from;

    public EmailSender(
        JavaMailSender mailSender,
        @Value("${spring.mail.username}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(
        String subject,
        String[] recipients,
        Attachment... attachments
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(from);
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(EMAIL_BODY);

            if (attachments.length > 0) {
                for (Attachment attachment : attachments) {
                    helper.addAttachment(attachment.filename, attachment.file);
                }
            }

            log.info(
                "About to send an email '{}' to {} recipients with {} attachments",
                subject,
                recipients.length,
                attachments.length
            );

            mailSender.send(message);
        } catch (MessagingException | MailException exc) {
            log.error("Error sending report", exc);
        }
    }
}
