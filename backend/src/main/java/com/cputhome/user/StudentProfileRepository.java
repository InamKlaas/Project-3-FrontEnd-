package com.cputhome.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/* persistence only, student-number uniqueness enforced here and in service */
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

  Optional<StudentProfile> findByStudentNumber(String studentNumber);

  boolean existsByStudentNumber(String studentNumber);
}
