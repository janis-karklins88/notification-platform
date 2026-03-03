package lv.janis.notification_platform.auth.adapter.in.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lv.janis.notification_platform.auth.application.port.out.ApiKeyRepositoryPort;
import lv.janis.notification_platform.auth.application.security.ApiKeyPrincipal;
import lv.janis.notification_platform.auth.application.service.ApiKeyHasher;
import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
  private static final String API_KEY_HEADER = "X-API-Key";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";

  private final ApiKeyRepositoryPort apiKeyRepositoryPort;
  private final ApiKeyHasher apiKeyHasher;

  public ApiKeyAuthenticationFilter(ApiKeyRepositoryPort apiKeyRepositoryPort, ApiKeyHasher apiKeyHasher) {
    this.apiKeyRepositoryPort = apiKeyRepositoryPort;
    this.apiKeyHasher = apiKeyHasher;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String rawKey = resolveRawApiKey(request);
    if (!StringUtils.hasText(rawKey)) {
      unauthorized(response);
      return;
    }

    String keyHash = apiKeyHasher.hash(rawKey);
    ApiKey apiKey = apiKeyRepositoryPort.findByKeyHash(keyHash).orElse(null);
    if (apiKey == null || apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
      unauthorized(response);
      return;
    }

    UUID tenantId = apiKey.getTenantId();
    if (tenantId == null) {
      unauthorized(response);
      return;
    }

    ApiKeyPrincipal principal = new ApiKeyPrincipal(apiKey.getId(), tenantId);
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null,
        List.of());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    filterChain.doFilter(request, response);
  }

  private String resolveRawApiKey(HttpServletRequest request) {
    String headerKey = request.getHeader(API_KEY_HEADER);
    if (StringUtils.hasText(headerKey)) {
      return headerKey.trim();
    }

    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (!StringUtils.hasText(authHeader)) {
      return null;
    }
    String normalized = authHeader.trim();
    if (normalized.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
        && normalized.length() > BEARER_PREFIX.length()) {
      return normalized.substring(BEARER_PREFIX.length()).trim();
    }
    return null;
  }

  private void unauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"message\":\"" + UNAUTHORIZED_MESSAGE + "\"}");
  }
}
