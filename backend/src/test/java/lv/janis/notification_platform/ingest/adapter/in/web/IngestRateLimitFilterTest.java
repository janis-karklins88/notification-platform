package lv.janis.notification_platform.ingest.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import lv.janis.notification_platform.auth.application.security.ApiKeyPrincipal;
import lv.janis.notification_platform.ingest.application.service.IngestRateLimiter;

class IngestRateLimitFilterTest {

  private final IngestRateLimiter ingestRateLimiter = mock(IngestRateLimiter.class);
  private final IngestRateLimitFilter filter = new IngestRateLimitFilter(ingestRateLimiter);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void allowsRequestAndAddsRateLimitHeadersWhenTokenAvailable() throws Exception {
    UUID apiKeyId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    var resetAt = Instant.parse("2026-04-14T10:00:30Z");
    when(ingestRateLimiter.check(apiKeyId)).thenReturn(new IngestRateLimiter.RateLimitDecision(
        true,
        100,
        99,
        Duration.ZERO,
        resetAt));
    withAuthentication(apiKeyId, tenantId);

    MockHttpServletRequest request = postIngestRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(ingestRateLimiter).check(apiKeyId);
    verify(chain).doFilter(request, response);
    assertEquals("100", response.getHeader(IngestRateLimitFilter.RATE_LIMIT_LIMIT_HEADER));
    assertEquals("99", response.getHeader(IngestRateLimitFilter.RATE_LIMIT_REMAINING_HEADER));
    assertEquals(String.valueOf(resetAt.getEpochSecond()),
        response.getHeader(IngestRateLimitFilter.RATE_LIMIT_RESET_HEADER));
  }

  @Test
  void rejectsRequestWhenBucketIsEmpty() throws Exception {
    UUID apiKeyId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    var resetAt = Instant.parse("2026-04-14T10:01:00Z");
    when(ingestRateLimiter.check(apiKeyId)).thenReturn(new IngestRateLimiter.RateLimitDecision(
        false,
        100,
        0,
        Duration.ofMillis(2500),
        resetAt));
    withAuthentication(apiKeyId, tenantId);

    MockHttpServletRequest request = postIngestRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(ingestRateLimiter).check(apiKeyId);
    verifyNoInteractions(chain);
    assertEquals(429, response.getStatus());
    assertEquals("3", response.getHeader(HttpHeaders.RETRY_AFTER));
    assertEquals("100", response.getHeader(IngestRateLimitFilter.RATE_LIMIT_LIMIT_HEADER));
    assertEquals("0", response.getHeader(IngestRateLimitFilter.RATE_LIMIT_REMAINING_HEADER));
    assertEquals(String.valueOf(resetAt.getEpochSecond()),
        response.getHeader(IngestRateLimitFilter.RATE_LIMIT_RESET_HEADER));
    assertEquals("{\"message\":\"Rate limit exceeded\"}", response.getContentAsString());
  }

  @Test
  void doesNotRateLimitGetEventRequests() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ingest/events/" + UUID.randomUUID());
    request.setServletPath("/ingest/events/" + UUID.randomUUID());
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    withAuthentication(UUID.randomUUID(), UUID.randomUUID());

    filter.doFilter(request, response, chain);

    verifyNoInteractions(ingestRateLimiter);
    verify(chain).doFilter(request, response);
  }

  private MockHttpServletRequest postIngestRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ingest");
    request.setServletPath("/ingest");
    return request;
  }

  private void withAuthentication(UUID apiKeyId, UUID tenantId) {
    var authentication = new UsernamePasswordAuthenticationToken(new ApiKeyPrincipal(apiKeyId, tenantId), null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
