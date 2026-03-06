package lv.janis.notification_platform.delivery.adapter.in.messaging;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.delivery.application.service.DeliveryProcessingService;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;

@Component
public class OutboxDlqListener {
  private static final Logger log = LoggerFactory.getLogger(OutboxDlqListener.class);

  private final DeliveryRepositoryPort deliveryRepositoryPort;
  private final DeliveryProcessingService deliveryProcessingService;
  private final Clock clock;

  public OutboxDlqListener(DeliveryRepositoryPort deliveryRepositoryPort,
      DeliveryProcessingService deliveryProcessingService, Clock clock) {
    this.deliveryRepositoryPort = deliveryRepositoryPort;
    this.deliveryProcessingService = deliveryProcessingService;
    this.clock = clock;
  }

  @RabbitListener(queues = OutboxMessagingConstants.QUEUE_OUTBOX_DLQ)
  public void onOutboxDlq(Message message) {
    MessageProperties messageProperties = message.getMessageProperties();
    String messageId = messageProperties.getMessageId();
    Map<String, Object> headers = messageProperties.getHeaders();

    String aggregateType = safeToString(headers.get(DeliveryListenerMessageExtractor.AGGREGATE_TYPE_HEADER));
    String aggregateIdText = safeToString(headers.get(DeliveryListenerMessageExtractor.AGGREGATE_ID_HEADER));

    if (!OutboxEventAggregateType.DELIVERY.name().equals(aggregateType)) {
      log.debug("DLQ message {} has non-delivery aggregateType={}, ignoring", messageId, aggregateType);
      return;
    }

    UUID deliveryId = parseDeliveryId(aggregateIdText);
    if (deliveryId == null) {
      log.warn("DLQ message {} has invalid delivery id '{}', cannot process", messageId, aggregateIdText);
      return;
    }

    deliveryRepositoryPort.findById(deliveryId).ifPresentOrElse(delivery -> {
      if (deliveryProcessingService.hasCompleted(delivery.getStatus())) {
        log.debug("DLQ delivery {} is already terminal ({})", deliveryId, delivery.getStatus());
        return;
      }
      String reason = buildReason(messageProperties);
      delivery.markFailed(clock.instant(), reason);
      deliveryRepositoryPort.save(delivery);
      log.info("Marked delivery {} as FAILED from DLQ reason={}", deliveryId, reason);
    }, () -> log.warn("DLQ message {} refers to missing delivery {}", messageId, deliveryId));
  }

  private UUID parseDeliveryId(String value) {
    try {
      return value == null ? null : UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private String buildReason(MessageProperties messageProperties) {
    Map<String, Object> headers = messageProperties.getHeaders();
    Object retryCount = extractRetryCount(headers);
    Object exceptionMessage = headers.get("x-exception-message");
    if (exceptionMessage == null) {
      exceptionMessage = headers.get("x-original-routing-key");
    }

    if (retryCount != null) {
      if (exceptionMessage != null) {
        return "DLQ for " + messageProperties.getConsumerQueue() + ", retryCount=" + retryCount + ", cause=" + exceptionMessage;
      }
      return "DLQ for " + messageProperties.getConsumerQueue() + ", retryCount=" + retryCount;
    }
    if (exceptionMessage != null) {
      return "DLQ for " + messageProperties.getConsumerQueue() + ", cause=" + exceptionMessage;
    }
    return "Message routed to DLQ for retry exhaustion";
  }

  @SuppressWarnings("unchecked")
  private Object extractRetryCount(Map<String, Object> headers) {
    Object xDeath = headers.get("x-death");
    if (!(xDeath instanceof List)) {
      return null;
    }
    List<Map<String, Object>> entries = (List<Map<String, Object>>) xDeath;
    if (entries.isEmpty()) {
      return null;
    }
    Object count = entries.get(0).get("count");
    return count == null ? null : count;
  }

  private String safeToString(Object value) {
    return value != null ? String.valueOf(value) : null;
  }
}
