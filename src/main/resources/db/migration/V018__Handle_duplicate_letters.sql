create table public.duplicates
(
    id                         uuid         not null
        constraint duplicate_pkey
            primary key,
    service                    varchar(256) not null,
    created_at                 timestamp    not null,
    additional_data            json,
    type                       varchar(256) not null,
    file_content               bytea,
    is_encrypted               boolean default false,
    checksum                   varchar(256) not null,
    encryption_key_fingerprint varchar(50),
    copies                     integer,
    is_async                   varchar(5)
);

INSERT INTO DUPLICATES (id, service, created_at, additional_data, type, file_content, is_encrypted, checksum, encryption_key_fingerprint, copies)
    SELECT id, service, created_at, additional_data, type, file_content, is_encrypted, checksum, encryption_key_fingerprint, copies
    FROM (
             SELECT row_number() over (PARTITION BY checksum, status) counter, id, service, created_at, additional_data, type, file_content, is_encrypted, checksum, encryption_key_fingerprint, copies
             FROM letters
             WHERE status = 'Created') as duplicatRecords
WHERE duplicatRecords.counter > 1;

DELETE FROM LETTERS WHERE id IN (SELECT id
from (
    SELECT row_number() over (PARTITION BY checksum, status) counter, id
    FROM letters
    WHERE status = 'Created') as duplicatRecords
WHERE duplicatRecords.counter> 1 );

