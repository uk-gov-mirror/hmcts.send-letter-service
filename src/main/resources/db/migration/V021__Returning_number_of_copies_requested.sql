ALTER TABLE letters
DROP COLUMN copies;

ALTER TABLE letters
ADD COLUMN copies Json;

ALTER TABLE duplicates
    DROP COLUMN copies;

ALTER TABLE duplicates
    ADD COLUMN copies Json;