-- Adds an optional sample/preview document URL (PDF) for the in-app book viewer.
ALTER TABLE books ADD COLUMN preview_url VARCHAR(512);
