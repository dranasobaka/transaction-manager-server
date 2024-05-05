--liquibase formatted sql

--changeset dkohut:create_resource_addresses_table splitStatements:false
CREATE TABLE IF NOT EXISTS resource_addresses
(
    id              UUID,
    address         BYTEA        NOT NULL,
    name            VARCHAR(200) NOT NULL,
    user_id         UUID         NOT NULL,
    manager_address BYTEA,
    permission_id   INTEGER,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT resource_addresses_pkey
        PRIMARY KEY (id),
    CONSTRAINT resource_address_address_key
        UNIQUE (address)
);
--rollback DROP TABLE IF EXISTS resource_addresses
