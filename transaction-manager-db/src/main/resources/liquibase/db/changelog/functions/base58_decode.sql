--liquibase formatted sql

--changeset base58_decode:0 splitStatements:false runOnChange:true
CREATE OR REPLACE FUNCTION base58_decode(str text)
    RETURNS bytea
    LANGUAGE 'plpgsql'
    COST 100
    IMMUTABLE PARALLEL UNSAFE
AS
$BODY$
DECLARE
    alphabet   CHAR(58) := '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';
    c          CHAR(1)  := null;
    p          INT      := null;
    raw_num    numeric  := 0;
    result_hex text;
BEGIN
    FOR i IN 1..CHAR_LENGTH(str)
        LOOP
            c = SUBSTRING(str FROM i FOR 1);
            p = POSITION(c IN alphabet);
            raw_num = (raw_num * 58) + (p - 1);
        END LOOP;
    RETURN numeric2bytea(raw_num);
END;
$BODY$;
--rollback empty
