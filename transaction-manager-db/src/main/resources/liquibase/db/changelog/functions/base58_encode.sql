--liquibase formatted sql

--changeset base58_encode:0 splitStatements:false runOnChange:true
CREATE OR REPLACE FUNCTION base58_encode(num numeric)
    RETURNS text
    LANGUAGE 'plpgsql'
    COST 100
    IMMUTABLE PARALLEL UNSAFE
AS
$BODY$
DECLARE
    alphabet text[]  = array [
        '1','2','3','4','5','6','7','8','9',
        'A','B','C','D','E','F','G','H','J','K','L','M','N','P','Q','R','S','T','U','V','W','X','Y','Z',
        'a','b','c','d','e','f','g','h','i','j','k','m','n','o','p','q','r','s','t','u','v','w','x','y','z'
        ];
    cnt      integer = 58;
    dst      text    = '';
    _mod     numeric;
BEGIN
    WHILE (num >= cnt)
        LOOP
            _mod = num % cnt;
            num = (num - _mod) / cnt;
            dst = alphabet[_mod + 1] || dst;
        END LOOP;
    RETURN alphabet[num + 1] || dst;
END;
$BODY$;
--rollback empty
