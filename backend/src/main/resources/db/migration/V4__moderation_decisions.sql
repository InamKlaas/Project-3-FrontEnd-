CREATE TABLE moderation_decisions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor VARCHAR(160) NOT NULL,
  subject_type VARCHAR(20) NOT NULL,
  subject_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  reason VARCHAR(500),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6),
  INDEX idx_moderation_subject (subject_type, subject_id)
);
