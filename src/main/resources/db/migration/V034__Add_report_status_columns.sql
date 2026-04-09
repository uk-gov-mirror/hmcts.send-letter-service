-- Add status and error_message columns to the reports table
ALTER TABLE reports
  ADD COLUMN status VARCHAR(10),
  ADD COLUMN error_message VARCHAR;

-- Set the status to 'SUCCESS' for all existing reports
UPDATE reports SET status = 'SUCCESS' WHERE status IS NULL;

-- Prevent NULL values in the status column going forward
ALTER TABLE reports
  ALTER COLUMN status SET NOT NULL;
