package lv.janis.notification_platform.ingest.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class IngestRateLimiter {
  private static final Logger log = LoggerFactory.getLogger(IngestRateLimiter.class);
  private static final String KEY_PREFIX = "rate-limit:ingest:api-key:";

  private static final DefaultRedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
      local capacity = tonumber(ARGV[1])
      local refill_window_ms = tonumber(ARGV[2])
      local now_ms = tonumber(ARGV[3])

      local values = redis.call('HMGET', KEYS[1], 'tokens', 'updatedAt')
      local tokens = tonumber(values[1])
      local updated_at = tonumber(values[2])

      if tokens == nil then
        tokens = capacity
      end
      if updated_at == nil then
        updated_at = now_ms
      end

      local elapsed_ms = now_ms - updated_at
      if elapsed_ms < 0 then
        elapsed_ms = 0
      end

      local refill = elapsed_ms * capacity / refill_window_ms
      tokens = math.min(capacity, tokens + refill)

      local allowed = 0
      local retry_after_ms = 0

      if tokens >= 1 then
        allowed = 1
        tokens = tokens - 1
      else
        retry_after_ms = math.ceil((1 - tokens) * refill_window_ms / capacity)
      end

      local remaining = math.floor(tokens)
      local reset_after_ms = math.ceil((capacity - tokens) * refill_window_ms / capacity)
      redis.call('HSET', KEYS[1], 'tokens', tokens, 'updatedAt', now_ms)
      redis.call('PEXPIRE', KEYS[1], refill_window_ms * 2)

      return { allowed, capacity, remaining, retry_after_ms, reset_after_ms }
      """, List.class);

  private final StringRedisTemplate redisTemplate;
  private final IngestRateLimitProperties properties;
  private final Clock clock;

  public IngestRateLimiter(
      StringRedisTemplate redisTemplate,
      IngestRateLimitProperties properties,
      Clock clock) {
    this.redisTemplate = redisTemplate;
    this.properties = properties;
    this.clock = clock;
  }

  public RateLimitDecision check(UUID apiKeyId) {
    if (!properties.enabled()) {
      return RateLimitDecision.allowed(properties.limit(), properties.limit(), Instant.now(clock));
    }

    try {
      Instant now = Instant.now(clock);
      List<?> result = redisTemplate.execute(
          TOKEN_BUCKET_SCRIPT,
          List.of(KEY_PREFIX + apiKeyId),
          String.valueOf(properties.limit()),
          String.valueOf(properties.window().toMillis()),
          String.valueOf(now.toEpochMilli()));

      if (result == null || result.size() != 5) {
        throw new IllegalStateException("Invalid Redis rate-limit script result");
      }

      boolean allowed = asLong(result.get(0)) == 1L;
      int limit = Math.toIntExact(asLong(result.get(1)));
      int remaining = Math.toIntExact(asLong(result.get(2)));
      Duration retryAfter = Duration.ofMillis(Math.max(asLong(result.get(3)), 0L));
      Instant resetAt = now.plusMillis(Math.max(asLong(result.get(4)), 0L));

      return new RateLimitDecision(allowed, limit, remaining, retryAfter, resetAt);
    } catch (RedisConnectionFailureException ex) {
      if (!properties.failOpen()) {
        throw ex;
      }
      log.warn("Redis unavailable for ingest rate limiting; allowing request apiKeyId={}", apiKeyId, ex);
      return RateLimitDecision.allowed(properties.limit(), properties.limit(), Instant.now(clock));
    }
  }

  private long asLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  public record RateLimitDecision(
      boolean allowed,
      int limit,
      int remaining,
      Duration retryAfter,
      Instant resetAt) {

    static RateLimitDecision allowed(int limit, int remaining, Instant resetAt) {
      return new RateLimitDecision(true, limit, remaining, Duration.ZERO, resetAt);
    }
  }
}
