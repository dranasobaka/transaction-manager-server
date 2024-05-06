--liquibase formatted sql

--changeset numeric2bytea:0 splitStatements:false runOnChange:true
CREATE OR REPLACE FUNCTION numeric2bytea(_n numeric)
    RETURNS bytea
    LANGUAGE 'plpgsql'
    COST 100
    IMMUTABLE STRICT PARALLEL UNSAFE
AS
$BODY$
DECLARE
    _b BYTEA := '\x';
    _v INTEGER;
BEGIN
    WHILE _n > 0
        LOOP
            _v := _n % 256;
            _b := SET_BYTE(('\x00' || _b), 0, _v);
            _n := (_n - _v) / 256;
        END LOOP;
    RETURN _b;
END;
$BODY$;
--rollback empty
