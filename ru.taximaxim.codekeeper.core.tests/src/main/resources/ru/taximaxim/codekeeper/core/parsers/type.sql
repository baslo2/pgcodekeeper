CREATE TYPE t1;
CREATE TYPE public.t1;
CREATE TYPE t1 AS (f1 int, f2 text[]);
CREATE TYPE t1 AS (f1 int, f2 text COLLATE "en_US");
CREATE TYPE t1 AS ENUM ();
CREATE TYPE t1 AS ENUM ('first', 'second', 'third');
CREATE TYPE t1 AS RANGE (SUBTYPE = float8);
CREATE TYPE t1 AS RANGE (SUBTYPE = float8, COLLATION = "en_US");
CREATE TYPE t1 AS RANGE (SUBTYPE = float8, CANONICAL = f1);
CREATE TYPE t1 AS RANGE (SUBTYPE = float8, SUBTYPE_DIFF = float8mi);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, RECEIVE = f3);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, SEND = f3);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, TYPMOD_IN = f3);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, TYPMOD_OUT = f3);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, ANALYZE = f3);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, INTERNALLENGTH = 16);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, PASSEDBYVALUE);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, ALIGNMENT = double);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, STORAGE = plain);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, LIKE = float);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, CATEGORY = 'U');
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, PREFERRED = true);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, DEFAULT = NULL);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, ELEMENT = float4);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, DELIMITER = ',');
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, COLLATABLE = true);
CREATE TYPE textrange1 AS RANGE(subtype=text, multirange_type_name=multirange_of_text, collation="C");
CREATE TYPE textrange2 AS RANGE(subtype=text, multirange_type_name=_textrange1, collation="C");

ALTER TYPE t1 ADD ATTRIBUTE f3 text;
ALTER TYPE t1 ADD ATTRIBUTE f3 text COLLATE "en_US";
ALTER TYPE t1 ADD ATTRIBUTE f3 text COLLATE "en_US" CASCADE;
ALTER TYPE t1 ADD ATTRIBUTE f3 text COLLATE "en_US" RESTRICT;
ALTER TYPE t1 DROP ATTRIBUTE f3;
ALTER TYPE t1 DROP ATTRIBUTE f3 CASCADE;
ALTER TYPE t1 DROP ATTRIBUTE f3 RESTRICT;
ALTER TYPE t1 DROP ATTRIBUTE IF EXISTS f3;
ALTER TYPE t1 DROP ATTRIBUTE IF EXISTS f3 CASCADE;
ALTER TYPE t1 DROP ATTRIBUTE IF EXISTS f3 RESTRICT;
ALTER TYPE t1 ALTER ATTRIBUTE f3 TYPE text;
ALTER TYPE t1 ALTER ATTRIBUTE f3 SET DATA TYPE text;
ALTER TYPE t1 ALTER ATTRIBUTE f3 TYPE text COLLATE "en_US";
ALTER TYPE t1 ALTER ATTRIBUTE f3 SET DATA TYPE text COLLATE "en_US";
ALTER TYPE t1 ALTER ATTRIBUTE f3 TYPE text CASCADE;
ALTER TYPE t1 ALTER ATTRIBUTE f3 SET DATA TYPE text CASCADE;
ALTER TYPE t1 ALTER ATTRIBUTE f3 TYPE text COLLATE "en_US" CASCADE;
ALTER TYPE t1 ALTER ATTRIBUTE f3 SET DATA TYPE text COLLATE "en_US" CASCADE;
ALTER TYPE t1 ALTER ATTRIBUTE f3 TYPE text RESTRICT;
ALTER TYPE t1 ALTER ATTRIBUTE f3 SET DATA TYPE text RESTRICT;
ALTER TYPE t1 ALTER ATTRIBUTE f3 TYPE text COLLATE "en_US" RESTRICT;
ALTER TYPE t1 ALTER ATTRIBUTE f3 SET DATA TYPE text COLLATE "en_US" RESTRICT;
ALTER TYPE t1 ADD ATTRIBUTE f3 text, ADD ATTRIBUTE f4 text, DROP ATTRIBUTE f2, ALTER ATTRIBUTE f1 TYPE text;
ALTER TYPE t1 OWNER TO test_user;
ALTER TYPE t1 OWNER TO CURRENT_USER;
ALTER TYPE t1 OWNER TO SESSION_USER;
ALTER TYPE t1 RENAME ATTRIBUTE f1 TO f2;
ALTER TYPE t1 RENAME ATTRIBUTE f1 TO f2 CASCADE;
ALTER TYPE t1 RENAME ATTRIBUTE f1 TO f2 RESTRICT;
ALTER TYPE t1 RENAME TO t2;
ALTER TYPE t1 SET SCHEMA new_schema;
ALTER TYPE t1 ADD VALUE 'fourth';
ALTER TYPE t1 ADD VALUE 'second' BEFORE 'third';
ALTER TYPE t1 ADD VALUE 'fourth' AFTER 'third';
ALTER TYPE t1 ADD VALUE IF NOT EXISTS 'second';
ALTER TYPE t1 ADD VALUE IF NOT EXISTS 'second' BEFORE 'third';
ALTER TYPE t1 ADD VALUE IF NOT EXISTS 'fourth' AFTER 'third';
ALTER TYPE t1 RENAME VALUE 'second' TO 'fourth';
ALTER TYPE t1 SET (RECEIVE = f3);
ALTER TYPE t1 SET (SEND = public.f3);
ALTER TYPE t1 SET (TYPMOD_IN = f3);
ALTER TYPE t1 SET (TYPMOD_OUT = f3);
ALTER TYPE t1 SET (ANALYZE = f3);
ALTER TYPE t1 SET (STORAGE = external);
ALTER TYPE t1 SET (STORAGE = extended);
ALTER TYPE t1 SET (STORAGE = main);
ALTER TYPE t1 SET (RECEIVE = NONE, SEND = NONE, TYPMOD_IN = NONE, TYPMOD_OUT = NONE, ANALYZE = NONE, STORAGE = plain);

DROP TYPE t1;
DROP TYPE IF EXISTS public.t1;
DROP TYPE t1, t2, public.t3;
DROP TYPE t1 CASCADE;
DROP TYPE t1 RESTRICT;
DROP TYPE t1, t2, public.t3 CASCADE;

-- cases only for GreenPlum

CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, COMPRESSTYPE = ZSTD);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, COMPRESSLEVEL = 19);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, BLOCKSIZE = 8192);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, COMPRESSTYPE = ZSTD, COMPRESSLEVEL = 19);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, COMPRESSTYPE = ZSTD, BLOCKSIZE = 8192);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, COMPRESSLEVEL = 19, BLOCKSIZE = 8192);
CREATE TYPE t1 (INPUT = f1, OUTPUT = f2, COMPRESSTYPE = ZSTD, COMPRESSLEVEL = 19, BLOCKSIZE = 8192);

CREATE TYPE public.int111 (
    INPUT = public.int111_in,
    OUTPUT = public.int111_out,
    INTERNALLENGTH = 4,
    PASSEDBYVALUE,
    ALIGNMENT = int4,
    STORAGE = plain,
    DEFAULT = '123'
);

ALTER TYPE public.int111 OWNER TO shamsutdinov_er;

ALTER TYPE public.int111 SET DEFAULT ENCODING (COMPRESSTYPE = none, BLOCKSIZE = 8192);

ALTER TYPE int33 SET DEFAULT ENCODING (compresstype=zlib, compresslevel=7, blocksize=16384);
ALTER TYPE int33 SET DEFAULT ENCODING (compresstype=zlib, compresslevel=7);
ALTER TYPE int33 SET DEFAULT ENCODING (compresslevel=7, blocksize=16384);
ALTER TYPE int33 SET DEFAULT ENCODING (compresstype=zlib, blocksize=16384);
ALTER TYPE int33 SET DEFAULT ENCODING (blocksize=16384);
ALTER TYPE int33 SET DEFAULT ENCODING (compresstype=zlib);
ALTER TYPE int33 SET DEFAULT ENCODING (compresslevel=7);