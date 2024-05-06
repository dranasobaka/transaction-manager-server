--liquibase formatted sql

--changeset dkohut:create_transactions_table splitStatements:false
CREATE TABLE IF NOT EXISTS transactions
(
    id              UUID         NOT NULL,
    order_id        UUID         NOT NULL,
    tx_id           VARCHAR(100) NOT NULL,
    to_address      BYTEA        NOT NULL,
    tx_amount       BIGINT,
    raw_transaction TEXT         NOT NULL,
    status          VARCHAR(30),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT transactions_pkey
        PRIMARY KEY (id),
    CONSTRAINT transactions_order_id__orders_id_fkey
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
);
--rollback DROP TABLE IF EXISTS transactions
