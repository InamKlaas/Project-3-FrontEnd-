package com.cputhome.security;

import com.cputhome.common.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/* stateless security, public doors are few and listed below.
 * the jwt filter joins in stage 3 with the user domain. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final CorsConfigurationSource cors;
  private final ObjectMapper mapper;

  public SecurityConfig(
      @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
      ObjectMapper mapper) {
    this.cors = cors;
    this.mapper = mapper;
  }

  @Bean
  public PasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(c -> c.configurationSource(cors))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> write(res, 401, "UNAUTHORIZED", "authentication required", req.getRequestURI()))
                    .accessDeniedHandler(
                        (req, res, ex) -> write(res, 403, "FORBIDDEN", "you may not perform this action", req.getRequestURI())))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll());
    return http.build();
  }

  /* entry point responses match our global error shape */

  private void write(HttpServletResponse res, int status, String code, String message, String path)
      throws java.io.IOException {
    res.setStatus(status);
    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(
        res.getOutputStream(),
        new ApiErrorResponse(Instant.now(), status, code, message, null, path));
  }
}
