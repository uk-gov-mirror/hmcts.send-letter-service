CREATE TABLE reports(
  id UUID,
  report_name VARCHAR(256),
  report_code VARCHAR(256),
  report_date DATE,
  printed_letters_count INTEGER,
  processed_at TIMESTAMP,
  is_international BOOLEAN,
  PRIMARY KEY (id)
);
