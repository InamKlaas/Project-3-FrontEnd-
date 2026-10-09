package com.cputhome.admin;

import com.cputhome.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/* Small persistent moderation trail; no FK so account removal preserves decisions. */
@Entity
@Table(name = "moderation_decisions")
public class ModerationDecision extends AuditableEntity {
  @Column(nullable = false, length = 160) private String actor;
  @Column(name = "subject_type", nullable = false, length = 20) private String subjectType;
  @Column(name = "subject_id", nullable = false) private Long subjectId;
  @Column(nullable = false, length = 30) private String action;
  @Column(length = 500) private String reason;

  protected ModerationDecision() {}

  public ModerationDecision(String actor, String subjectType, Long subjectId, String action, String reason) {
    this.actor = actor;
    this.subjectType = subjectType;
    this.subjectId = subjectId;
    this.action = action;
    this.reason = reason;
  }

  public String getActor() { return actor; }
  public String getSubjectType() { return subjectType; }
  public Long getSubjectId() { return subjectId; }
  public String getAction() { return action; }
  public String getReason() { return reason; }
}
