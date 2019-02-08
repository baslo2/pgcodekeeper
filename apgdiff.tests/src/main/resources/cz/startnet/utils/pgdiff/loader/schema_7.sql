CREATE SCHEMA common;

CREATE FUNCTION common.t_common_casttotext(time with time zone) RETURNS text
    AS $_$SELECT textin(timetz_out($1));$_$
    LANGUAGE sql IMMUTABLE STRICT;

CREATE FUNCTION common.t_common_casttotext(time without time zone) RETURNS text
    AS $_$SELECT textin(time_out($1));$_$
    LANGUAGE sql IMMUTABLE STRICT;

CREATE FUNCTION common.t_common_casttotext(timestamp with time zone) RETURNS text
    AS $_$SELECT textin(timestamptz_out($1));$_$
    LANGUAGE sql IMMUTABLE STRICT;

CREATE FUNCTION common.t_common_casttotext(timestamp without time zone) RETURNS text
    AS $_$SELECT textin(timestamp_out($1));$_$
    LANGUAGE sql IMMUTABLE STRICT;