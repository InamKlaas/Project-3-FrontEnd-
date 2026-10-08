package com.cputhome.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/* persistence only, verification transitions live in the admin service */
public interface LandlordProfileRepository extends JpaRepository<LandlordProfile, Long> {

  Optional<LandlordProfile> findByUserId(Long userId);
}
