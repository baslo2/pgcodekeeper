-- DEPCY: This COLUMN b depends on the COLUMN: default.t2.d

ALTER TABLE default.t2 MODIFY COLUMN `b` UInt8 TTL (c + toIntervalDay(1));

ALTER TABLE default.t2
	DROP COLUMN `d`;

-- DEPCY: This COLUMN c depends on the COLUMN: default.t4.d

ALTER TABLE default.t4 MODIFY COLUMN `c` ALIAS b + a;

ALTER TABLE default.t4
	DROP COLUMN `d`;

ALTER TABLE default.t MODIFY COLUMN `b` Int64;

ALTER TABLE default.t MODIFY COLUMN `c` Int64 TTL col11 + toIntervalDay(1);

ALTER TABLE default.t MODIFY COLUMN `c1` REMOVE DEFAULT;

ALTER TABLE default.t MODIFY COLUMN `c2` ALIAS b + a;

ALTER TABLE default.t MODIFY COLUMN `c2` REMOVE CODEC;

ALTER TABLE default.t MODIFY COLUMN `c3` DEFAULT b + a + c;

ALTER TABLE default.t MODIFY COLUMN `c3` REMOVE COMMENT;

ALTER TABLE default.t MODIFY COLUMN `c4` DEFAULT 0;

ALTER TABLE default.t MODIFY COLUMN `c4` REMOVE DEFAULT;

ALTER TABLE default.t COMMENT COLUMN `col13` 'test';

ALTER TABLE default.t1 MODIFY COLUMN `b` UInt8;

ALTER TABLE default.t1 MODIFY COLUMN `c` REMOVE TTL;

ALTER TABLE default.t1 MODIFY COLUMN `c1` DEFAULT false;

ALTER TABLE default.t1 MODIFY COLUMN `c2` DEFAULT b + a;

ALTER TABLE default.t1 MODIFY COLUMN `c2` CODEC(LZ4HC(0));

ALTER TABLE default.t1 MODIFY COLUMN `c3` DEFAULT b + a;

ALTER TABLE default.t1 COMMENT COLUMN `c3` 'test comment';

ALTER TABLE default.t1 MODIFY COLUMN `c4` EPHEMERAL b + a;

ALTER TABLE default.t1 MODIFY COLUMN `col13` REMOVE COMMENT;