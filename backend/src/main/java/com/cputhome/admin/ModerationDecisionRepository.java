package com.cputhome.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationDecisionRepository extends JpaRepository<ModerationDecision, Long> {
  List<ModerationDecision> findBySubjectTypeAndSubjectIdOrderByIdAsc(String type, Long id);
}
