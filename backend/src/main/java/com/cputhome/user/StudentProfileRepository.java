package com.cputhome.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/* persistence only, student-number uniqueness enforced here and in service */
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

  Optional<StudentProfile> findByStudentNumber(String studentNumber);

  Optional<StudentProfile> findByUserId(Long userId);

  boolean existsByStudentNumber(String studentNumber);
}
