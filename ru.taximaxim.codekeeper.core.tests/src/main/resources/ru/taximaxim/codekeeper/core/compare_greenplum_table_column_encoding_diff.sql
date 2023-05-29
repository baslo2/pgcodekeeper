SET search_path = pg_catalog;

ALTER TABLE public.test_table_elm1
	ADD COLUMN col2 text ENCODING (COMPRESSTYPE = none, COMPRESSLEVEL = 0, BLOCKSIZE = 32768);

ALTER TABLE public.test_table_elm1
	ADD COLUMN col3 text NOT NULL ENCODING (COMPRESSTYPE = none, COMPRESSLEVEL = 1, BLOCKSIZE = 32768);

ALTER TABLE ONLY public.test_table_elm1
	DROP COLUMN col1;

ALTER TABLE public.test_table_elm1
	ADD COLUMN col1 text NOT NULL ENCODING (COMPRESSTYPE = zlib, COMPRESSLEVEL = 2, BLOCKSIZE = 32768);
