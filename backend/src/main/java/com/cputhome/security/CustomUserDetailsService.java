package com.cputhome.security;

import com.cputhome.user.User;
import com.cputhome.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/* loads by numeric id (jwt subject) for method-security expressions */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository users;

  public CustomUserDetailsService(UserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
    Long userId;
    try {
      userId = Long.parseLong(id);
    } catch (NumberFormatException e) {
      throw new UsernameNotFoundException("user not found");
    }
    Optional<User> found = users.findById(userId);
    if (found.isEmpty()) {
      throw new UsernameNotFoundException("user not found");
    }
    User user = found.get();
    return new org.springframework.security.core.userdetails.User(
        user.getId().toString(),
        user.getPasswordHash(),
        user.isEnabled(),
        true,
        true,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
  }
}
