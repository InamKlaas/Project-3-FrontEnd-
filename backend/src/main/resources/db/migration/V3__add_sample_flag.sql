-- V3: synthetic-seed marker on accommodations (provider rows stay false)
ALTER TABLE accommodations ADD COLUMN sample BIT(1) NOT NULL DEFAULT b'0';
