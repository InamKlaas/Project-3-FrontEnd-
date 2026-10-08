-- V1: users, profiles, accommodations, rooms, subtypes
CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  password_hash VARCHAR(120) NOT NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(30) NOT NULL,
  enabled BIT(1) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_role ON users (role);

CREATE TABLE student_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  student_number VARCHAR(30) NOT NULL,
  campus VARCHAR(80) NOT NULL,
  year_of_study VARCHAR(10) NULL,
  funding VARCHAR(30) NULL,
  email_verified BIT(1) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT uq_student_number UNIQUE (student_number)
);

CREATE TABLE landlord_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  verification_status VARCHAR(20) NOT NULL,
  registration_number VARCHAR(60) NULL,
  proof_url VARCHAR(500) NULL,
  accreditation BIT(1) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_landlord_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_landlord_verification ON landlord_profiles (verification_status);

CREATE TABLE accommodations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  description VARCHAR(4000) NOT NULL,
  location VARCHAR(200) NOT NULL,
  campus VARCHAR(80) NOT NULL,
  address VARCHAR(300) NULL,
  on_campus BIT(1) NOT NULL,
  nsfas_claim BIT(1) NOT NULL,
  gender VARCHAR(20) NULL,
  house_rules VARCHAR(2000) NULL,
  shuttle VARCHAR(500) NULL,
  utilities DECIMAL(12, 2) NULL,
  is_published BIT(1) NOT NULL,
  is_active BIT(1) NOT NULL,
  approval_status VARCHAR(20) NOT NULL,
  rejection_reason VARCHAR(500) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_accommodation_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_accommodation_owner ON accommodations (owner_id);
CREATE INDEX idx_accommodation_campus ON accommodations (campus);
CREATE INDEX idx_accommodation_location ON accommodations (location);
CREATE INDEX idx_accommodation_visibility ON accommodations (is_published, is_active, approval_status);

CREATE TABLE accommodation_amenities (
  accommodation_id BIGINT NOT NULL,
  amenity VARCHAR(60) NOT NULL,
  CONSTRAINT fk_amenities_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id) ON DELETE CASCADE
);

CREATE TABLE accommodation_images (
  accommodation_id BIGINT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  CONSTRAINT fk_images_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id) ON DELETE CASCADE
);

CREATE TABLE room_listings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  accommodation_id BIGINT NOT NULL,
  room_type VARCHAR(30) NOT NULL,
  monthly_rent DECIMAL(12, 2) NOT NULL,
  deposit DECIMAL(12, 2) NULL,
  beds INT NOT NULL,
  available BIT(1) NOT NULL,
  available_date DATE NULL,
  emergency BIT(1) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_room_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id) ON DELETE CASCADE
);

CREATE INDEX idx_room_accommodation ON room_listings (accommodation_id);
CREATE INDEX idx_room_availability ON room_listings (available, monthly_rent);
CREATE INDEX idx_room_emergency ON room_listings (emergency, available);

CREATE TABLE shared_accommodations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  accommodation_id BIGINT NOT NULL,
  total_beds INT NOT NULL,
  shared_kitchen BIT(1) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_shared_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id) ON DELETE CASCADE,
  CONSTRAINT uq_shared_accommodation UNIQUE (accommodation_id)
);

CREATE TABLE private_rooms (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  accommodation_id BIGINT NOT NULL,
  ensuite BIT(1) NOT NULL,
  furnished BIT(1) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_private_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id) ON DELETE CASCADE,
  CONSTRAINT uq_private_accommodation UNIQUE (accommodation_id)
);

CREATE TABLE entire_units (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  accommodation_id BIGINT NOT NULL,
  bedrooms INT NOT NULL,
  bathrooms INT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_entire_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodations (id) ON DELETE CASCADE,
  CONSTRAINT uq_entire_accommodation UNIQUE (accommodation_id)
);
