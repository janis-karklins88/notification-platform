package lv.janis.notification_platform.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.auth.application.port.out.ApiKeyRepositoryPort;
import lv.janis.notification_platform.auth.application.service.ApiKeyHasher;
import lv.janis.notification_platform.ingest.adapter.in.web.IngestRateLimitFilter;
import lv.janis.notification_platform.ingest.application.service.IngestRateLimiter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
      ApiKeyRepositoryPort apiKeyRepositoryPort,
      ApiKeyHasher apiKeyHasher) {
    return new ApiKeyAuthenticationFilter(apiKeyRepositoryPort, apiKeyHasher);
  }

  @Bean
  IngestRateLimitFilter ingestRateLimitFilter(IngestRateLimiter ingestRateLimiter) {
    return new IngestRateLimitFilter(ingestRateLimiter);
  }

  @Bean
  @Order(1)
  @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
  SecurityFilterChain adminSecurityWithJwt(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/admin/**")
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

  @Bean
  @Order(1)
  @ConditionalOnMissingBean(name = "adminSecurityWithJwt")
  SecurityFilterChain adminSecurityWithoutJwt(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/admin/**")
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain ingestSecurity(
      HttpSecurity http,
      ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
      IngestRateLimitFilter ingestRateLimitFilter)
      throws Exception {
    return http
        .securityMatcher("/ingest/**")
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .addFilterBefore(apiKeyAuthenticationFilter, AnonymousAuthenticationFilter.class)
        .addFilterAfter(ingestRateLimitFilter, ApiKeyAuthenticationFilter.class)
        .build();
  }

  @Bean
  @Order(3)
  SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
    return http
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/boot-check", "/actuator/health", "/actuator/info").permitAll()
            .anyRequest().permitAll())
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOriginsProperty) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.stream(allowedOriginsProperty.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-API-Key"));
    configuration.setExposedHeaders(List.of(
        "Location",
        "Retry-After",
        IngestRateLimitFilter.RATE_LIMIT_LIMIT_HEADER,
        IngestRateLimitFilter.RATE_LIMIT_REMAINING_HEADER,
        IngestRateLimitFilter.RATE_LIMIT_RESET_HEADER));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new RolesClaimAuthoritiesConverter());
    return converter;
  }

  private static class RolesClaimAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      Set<GrantedAuthority> authorities = new LinkedHashSet<>();
      Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);
      if (scopeAuthorities != null) {
        authorities.addAll(scopeAuthorities);
      }

      authorities.addAll(extractRoleAuthorities(jwt.getClaim("roles")));

      Object realmAccess = jwt.getClaim("realm_access");
      if (realmAccess instanceof Map<?, ?> realmAccessMap) {
        authorities.addAll(extractRoleAuthorities(realmAccessMap.get("roles")));
      }

      Object resourceAccess = jwt.getClaim("resource_access");
      if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
        for (Object resourceValue : resourceAccessMap.values()) {
          if (resourceValue instanceof Map<?, ?> clientAccessMap) {
            authorities.addAll(extractRoleAuthorities(clientAccessMap.get("roles")));
          }
        }
      }

      return authorities;
    }

    private List<GrantedAuthority> extractRoleAuthorities(Object rolesClaim) {
      List<GrantedAuthority> roles = new ArrayList<>();
      if (rolesClaim instanceof Collection<?> roleValues) {
        for (Object roleValue : roleValues) {
          if (roleValue == null) {
            continue;
          }
          String role = roleValue.toString().trim();
          if (role.isEmpty()) {
            continue;
          }
          String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
          roles.add(new SimpleGrantedAuthority(prefixedRole));
        }
      }
      return roles;
    }
  }
}
