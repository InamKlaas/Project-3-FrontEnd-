package com.cputhome.security;

import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/* reads bearer tokens, never logs them, anonymous when missing or bad */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwt;
  private final UserRepository users;

  public JwtAuthenticationFilter(JwtService jwt, UserRepository users) {
    this.jwt = jwt;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      if (!token.isBlank() && jwt.valid(token)) {
        try {
          UserPrincipal claimed = jwt.principal(token);
          User user = users.findById(claimed.id()).orElse(null);
          /* disabled accounts lose access immediately even with a live token */
          if (user != null && user.isEnabled()) {
            UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), user.getRole());
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
          }
        } catch (Exception e) {
          SecurityContextHolder.clearContext();
        }
      }
    }
    chain.doFilter(request, response);
  }
}
