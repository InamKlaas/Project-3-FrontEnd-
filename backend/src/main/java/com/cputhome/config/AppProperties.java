package com.cputhome.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/* typed view of our app.* keys, avoids @Value soup in services */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Cors cors, Jwt jwt, Media media, Admin admin) {

  public record Cors(String allowedOrigins) {}

  public record Jwt(String secret, long expiryMs) {}

  public record Media(String uploadDir) {}

  public record Admin(String email, String password) {}
}
