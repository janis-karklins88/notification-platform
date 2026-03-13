package lv.janis.notification_platform.delivery.adapter.out.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.application.model.PreparedEmailMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class EmailSenderAdapterTest {
  private final JavaMailSender mailSender = mock(JavaMailSender.class);
  private final EmailSenderAdapter adapter = new EmailSenderAdapter(mailSender);

  @Test
  void sendBuildsMimeMessageAndDelegatesToMailSender() throws Exception {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    PreparedEmailMessage message = new PreparedEmailMessage(
        List.of("to1@example.com", "to2@example.com"),
        "from@example.com",
        "reply@example.com",
        "Subject",
        "<p>Hello</p>",
        true);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    adapter.send(message);

    assertEquals("Subject", mimeMessage.getSubject());
    assertEquals(2, mimeMessage.getAllRecipients().length);
    assertEquals("from@example.com", ((InternetAddress) mimeMessage.getFrom()[0]).getAddress());
    assertEquals("reply@example.com", ((InternetAddress) mimeMessage.getReplyTo()[0]).getAddress());
    verify(mailSender).send(mimeMessage);
  }

  @Test
  void sendWrapsInvalidMessageBuildAsNonRetryable() {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    PreparedEmailMessage message = new PreparedEmailMessage(
        List.of("not a valid email"),
        "",
        "",
        "Subject",
        "Body",
        false);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    DeliveryNonRetryableException ex = assertThrows(DeliveryNonRetryableException.class, () -> adapter.send(message));

    assertEquals("Failed to build email message", ex.getMessage());
  }

  @Test
  void sendWrapsNonRetryableMailException() {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    PreparedEmailMessage message = new PreparedEmailMessage(List.of("to@example.com"), "", "", "Subject", "Body", false);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    doThrow(new MailAuthenticationException("auth")).when(mailSender).send(mimeMessage);

    DeliveryNonRetryableException ex = assertThrows(DeliveryNonRetryableException.class, () -> adapter.send(message));

    assertEquals("Non-retryable email delivery failure", ex.getMessage());
  }

  @Test
  void sendRethrowsRetryableMailException() {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    PreparedEmailMessage message = new PreparedEmailMessage(List.of("to@example.com"), "", "", "Subject", "Body", false);
    MailSendException failure = new MailSendException("temporary");
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    doThrow(failure).when(mailSender).send(mimeMessage);

    MailSendException ex = assertThrows(MailSendException.class, () -> adapter.send(message));

    assertEquals(failure, ex);
  }
}
