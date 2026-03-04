package lv.janis.notification_platform.outbox.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.dispatch")
public record OutboxDispatchProperties(int batchSize, boolean enabled, long fixedDelayMs) {

}
