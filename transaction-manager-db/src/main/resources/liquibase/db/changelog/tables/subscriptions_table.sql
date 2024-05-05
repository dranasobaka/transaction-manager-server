--liquibase formatted sql

--changeset dkohut:create_subscriptions_table splitStatements:false
CREATE TABLE IF NOT EXISTS subscriptions
(
    event_id                   VARCHAR(40)   NOT NULL,
    event_type                 VARCHAR(60)   NOT NULL,
    service_name               VARCHAR(60)   NOT NULL,
    subscription_type          VARCHAR(10)   NOT NULL,
    s_partition                INTEGER       NOT NULL,
    trigger_ts_millis          BIGINT        NOT NULL,
    payload                    VARCHAR(8000) NOT NULL,
    versioning                 INTEGER DEFAULT 0,
    handled                    BOOLEAN DEFAULT false,
    partition_key              VARCHAR(61),
    schedule_cron              VARCHAR(40),
    start_ts_millis            BIGINT,
    end_ts_millis              BIGINT,
    schedule_fixed_rate_millis INTEGER,
    CONSTRAINT subscriptions_pkey
        PRIMARY KEY (event_id, event_type)
);
--rollback DROP TABLE IF EXISTS subscriptions

--changeset dkohut:create_subscription_type_index splitStatements:false
CREATE INDEX IF NOT EXISTS idx_subscription_type
    ON subscriptions (subscription_type);
--rollback DROP INDEX IF EXISTS idx_subscription_type
