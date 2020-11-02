create table public.exception
(
    id                         uuid         not null
        constraint exception_pkey
            primary key,
    service                    varchar(256) not null,
    created_at                 timestamp ,
    type                       varchar(256) ,
    message                    varchar(256) ,
    is_async                   varchar(5)
);