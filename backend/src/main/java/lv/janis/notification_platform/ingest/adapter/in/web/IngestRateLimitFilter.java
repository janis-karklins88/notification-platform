package lv.janis.notification_platform.ingest.adapter.in.web;

import java.io.IOException;
import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lv.janis.notification_platform.auth.application.security.ApiKeyPrincipal;
import lv.janis.notification_platform.ingest.application.service.IngestRateLimiter;

public class IngestRateLimitFilter extends OncePerRequestFilter {
  public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
  public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
  public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

  private static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded";

  private final IngestRateLimiter ingestRateLimiter;

  public IngestRateLimitFilter(IngestRateLimiter ingestRateLimiter) {
    this.ingestRateLimiter = ingestRateLimiter;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    ApiKeyPrincipal principal = currentPrincipal();
    if (principal == null) {
      filterChain.doFilter(request, response);
      return;
    }

    var decision = ingestRateLimiter.check(principal.apiKeyId());
    addRateLimitHeaders(response, decision);

    if (!decision.allowed()) {
      reject(response, decision);
      return;
    }

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"POST".equalsIgnoreCase(request.getMethod()) || !"/ingest".equals(request.getServletPath());
  }

  private ApiKeyPrincipal currentPrincipal() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof ApiKeyPrincipal principal)) {
      return null;
    }
    return principal;
  }

  private void addRateLimitHeaders(HttpServletResponse response, IngestRateLimiter.RateLimitDecision decision) {
    response.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf(decision.limit()));
    response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(decision.remaining()));
    response.setHeader(RATE_LIMIT_RESET_HEADER, String.valueOf(decision.resetAt().getEpochSecond()));
  }

  private void reject(HttpServletResponse response, IngestRateLimiter.RateLimitDecision decision) throws IOException {
    response.setStatus(429);
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(toSecondsCeiling(decision.retryAfter())));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write("{\"message\":\"" + RATE_LIMIT_EXCEEDED_MESSAGE + "\"}");
  }

  private long toSecondsCeiling(Duration duration) {
    long millis = Math.max(duration.toMillis(), 0L);
    if (millis == 0L) {
      return 0L;
    }
    return (millis + 999L) / 1000L;
  }
}
