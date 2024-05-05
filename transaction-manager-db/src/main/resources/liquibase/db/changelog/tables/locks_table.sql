--liquibase formatted sql

--changeset dkohut:create_locks_table splitStatements:false
CREATE TABLE IF NOT EXISTS locks
(
    s_partition        INTEGER,
    locked_by_hostname VARCHAR(255) NOT NULL,
    locked_until_ts    TIMESTAMP DEFAULT now(),
    CONSTRAINT locks_pkey
        PRIMARY KEY (s_partition)
);
--rollback DROP TABLE IF EXISTS locks