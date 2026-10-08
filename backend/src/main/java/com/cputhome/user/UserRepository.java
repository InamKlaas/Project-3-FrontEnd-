package com.cputhome.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/* persistence only, identity rules live in the auth service */
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
