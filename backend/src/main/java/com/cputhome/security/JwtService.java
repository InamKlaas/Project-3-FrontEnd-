package com.cputhome.security;

import com.cputhome.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/* only place that mints or reads tokens, secret comes from env */
@Service
public class JwtService {

  private final SecretKey key;
  private final long expiryMs;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiry-ms:86400000}") long expiryMs) {
    /* hmac needs a long key, fail fast if someone sets a tiny one */
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiryMs = expiryMs;
  }

  /* subject is the numeric user id, email + role ride along as claims */

  public String generate(User user) {
    Date now = new Date();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole().name())
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expiryMs))
        .signWith(key)
        .compact();
  }

  public boolean valid(String token) {
    try {
      parse(token);
      return true;
    } catch (Exception e) {
      /* expired or tampered, caller treats both as unauthenticated */
      return false;
    }
  }

  public UserPrincipal principal(String token) {
    Claims claims = parse(token);
    return new UserPrincipal(
        Long.parseLong(claims.getSubject()),
        claims.get("email", String.class),
        com.cputhome.user.UserRole.valueOf(claims.get("role", String.class)));
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
