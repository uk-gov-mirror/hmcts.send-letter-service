UPDATE letters
SET checksum = message_id
WHERE checksum IS NULL;

ALTER TABLE letters
ALTER COLUMN checksum SET NOT NULL;
