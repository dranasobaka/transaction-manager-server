--liquibase formatted sql

--changeset dkohut:initial_resource_addresses splitStatements:false
INSERT INTO resource_addresses (id, name, address, user_id, manager_address, permission_id, active)
VALUES ('74c405a3-47c6-4004-aaf5-11be5ea2c5be', 'TransaTron resources', tronbytes20('TTts2zYrvdvNNshqausfbNWJy24HJZ1jYG'),
        '0a121df2-bde6-41f3-a731-ef1e32ecca6d', tronbytes20('TVr5gtvb85XCzBuHUvmUUgsjwwdnBMAkos'), 3, true)
ON CONFLICT DO NOTHING;
--rollback empty
