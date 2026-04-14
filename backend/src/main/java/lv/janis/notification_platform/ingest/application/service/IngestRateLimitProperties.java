package lv.janis.notification_platform.ingest.application.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit.ingest")
public record IngestRateLimitProperties(
    boolean enabled,
    int limit,
    Duration window,
    boolean failOpen) {

  public IngestRateLimitProperties {
    if (limit < 1) {
      throw new IllegalArgumentException("Ingest rate limit must be at least 1");
    }
    if (window == null || window.isZero() || window.isNegative()) {
      throw new IllegalArgumentException("Ingest rate-limit window must be positive");
    }
  }
}
