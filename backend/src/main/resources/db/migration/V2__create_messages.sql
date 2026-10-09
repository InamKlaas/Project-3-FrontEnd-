-- V2: student-landlord conversations, thread identity is listing + student
CREATE TABLE messages (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  listing_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  body VARCHAR(2000) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_messages_listing FOREIGN KEY (listing_id) REFERENCES accommodations (id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_student FOREIGN KEY (student_id) REFERENCES users (id),
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE INDEX idx_messages_thread ON messages (listing_id, student_id, created_at);
CREATE INDEX idx_messages_student ON messages (student_id, created_at);
