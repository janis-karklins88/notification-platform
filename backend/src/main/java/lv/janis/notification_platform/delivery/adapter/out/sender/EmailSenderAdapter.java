package lv.janis.notification_platform.delivery.adapter.out.sender;

import java.util.Objects;

import jakarta.mail.SendFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
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
      if (isNonRetryableMailException(ex)) {
        throw new DeliveryNonRetryableException("Non-retryable email delivery failure", ex);
      }
      throw ex;
    }
  }

  private static boolean isNonRetryableMailException(MailException ex) {
    Throwable cursor = ex;
    while (cursor != null) {
      if (cursor instanceof MailAuthenticationException
          || cursor instanceof MailPreparationException
          || cursor instanceof MailParseException
          || cursor instanceof SendFailedException
          || cursor instanceof MailSendException sendException
          && hasPermanentFailures(sendException)) {
        return true;
      }
      cursor = cursor.getCause();
    }
    return false;
  }

  private static boolean hasPermanentFailures(MailSendException ex) {
    var failedMessages = ex.getFailedMessages();
    return failedMessages != null && !failedMessages.isEmpty();
  }
}
