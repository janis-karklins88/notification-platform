package lv.janis.notification_platform.delivery.adapter.out.sender;

import java.util.Objects;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.application.model.PreparedEmailMessage;
import lv.janis.notification_platform.delivery.application.port.out.EmailSenderPort;

@Component
public class EmailSenderAdapter implements EmailSenderPort {
  private final JavaMailSender mailSender;

  public EmailSenderAdapter(JavaMailSender mailSender) {
    this.mailSender = Objects.requireNonNull(mailSender, "mailSender must not be null");
  }

  @Override
  public void send(PreparedEmailMessage message) {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setTo(message.recipients().toArray(new String[0]));
      helper.setSubject(message.subject());
      helper.setText(message.body(), message.html());

      if (message.from() != null && !message.from().isBlank()) {
        helper.setFrom(message.from());
      }
      if (message.replyTo() != null && !message.replyTo().isBlank()) {
        helper.setReplyTo(message.replyTo());
      }

      mailSender.send(mimeMessage);
    } catch (MessagingException ex) {
      throw new DeliveryNonRetryableException("Failed to build email message", ex);
    } catch (MailException ex) {
      throw ex;
    }
  }
}
