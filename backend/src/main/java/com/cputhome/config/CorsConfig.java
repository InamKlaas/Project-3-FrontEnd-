package com.cputhome.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/* cors stays locked down, origins come from env so prod never inherits localhost */
@Configuration
public class CorsConfig {

  private final List<String> allowedOrigins;

  public CorsConfig(
      @Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {
    /* comma separated in env, single value locally */
    this.allowedOrigins =
        Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
