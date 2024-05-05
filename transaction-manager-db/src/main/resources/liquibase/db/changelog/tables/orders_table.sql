--liquibase formatted sql

--changeset dkohut:create_orders_table splitStatements:false
CREATE TABLE IF NOT EXISTS orders
(
    id              UUID        NOT NULL,
    wallet_address  BYTEA       NOT NULL,
    fulfill_from    TIMESTAMP   NOT NULL,
    fulfill_to      TIMESTAMP   NOT NULL,
    status          VARCHAR(30) NOT NULL,
    own_energy      BIGINT,
    external_energy BIGINT,
    own_bandwidth   BIGINT,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT orders_pkey
        PRIMARY KEY (id)
);
--rollback DROP TABLE IF EXISTS orders
