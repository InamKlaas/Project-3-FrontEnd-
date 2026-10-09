CREATE TABLE housing_entries (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  kind VARCHAR(20) NOT NULL,
  listing_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  note VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL,
  rating INT NULL,
  move_in DATE NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NULL,
  CONSTRAINT uq_housing_entry UNIQUE (kind, listing_id, author_id),
  CONSTRAINT fk_entry_listing FOREIGN KEY (listing_id) REFERENCES accommodations(id) ON DELETE CASCADE,
  CONSTRAINT fk_entry_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE application_documents (
  entry_id BIGINT NOT NULL,
  document VARCHAR(400) NOT NULL,
  CONSTRAINT fk_document_entry FOREIGN KEY (entry_id) REFERENCES housing_entries(id) ON DELETE CASCADE
);
