--liquibase formatted sql

--changeset tronbytes20:0 splitStatements:false runOnChange:true
CREATE OR REPLACE FUNCTION tronbytes20(address text)
    RETURNS bytea
    LANGUAGE 'plpgsql'
    COST 150
    IMMUTABLE PARALLEL UNSAFE
AS
$BODY$
DECLARE
BEGIN
    RETURN substring(base58_decode(address) FROM 2 FOR 20);
END;
$BODY$;
--rollback empty
