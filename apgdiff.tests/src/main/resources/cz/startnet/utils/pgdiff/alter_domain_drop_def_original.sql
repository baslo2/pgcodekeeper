CREATE DOMAIN public.dom2 AS integer NOT NULL DEFAULT (-100)
	CONSTRAINT dom2_check CHECK ((VALUE < 1000));

ALTER DOMAIN public.dom2 OWNER TO botov_av;