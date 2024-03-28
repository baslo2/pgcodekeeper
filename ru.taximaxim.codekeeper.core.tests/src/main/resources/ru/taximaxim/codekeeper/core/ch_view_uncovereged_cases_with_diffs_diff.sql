CREATE VIEW default.v_c_1
(
    `s` UInt8,
    `col1` Int64,
    `col2` Int64,
    CONSTRAINT c_1 CHECK col1 > 1
) AS
SELECT 1 AS s;

CREATE VIEW default.v_i_1
(
    `s` UInt8,
    `col1` Int64,
    `col2` Int64,
    INDEX i_1 col1 TYPE minmax GRANULARITY 1
) AS
SELECT 1 AS s;

DROP VIEW default.v_c_2;

CREATE VIEW default.v_c_2
(
    `s` UInt8,
    `col1` Int64,
    `col2` Int64,
    CONSTRAINT c_2 CHECK col1 > 1
) AS
SELECT 1 AS s;

DROP VIEW default.v_i_2;

CREATE VIEW default.v_i_2
(
    `s` UInt8,
    `col1` Int64,
    `col2` Int64,
    INDEX i_2 col1 TYPE minmax GRANULARITY 1
) AS
SELECT 1 AS s;

DROP VIEW default.v_c_3;

CREATE VIEW default.v_c_3
(
    `s` UInt8,
    `col1` Int64,
    `col2` Int64,
    CONSTRAINT c_3 CHECK (col1 + col2) > 1
) AS
SELECT 1 AS s;

DROP VIEW default.v_i_3;

CREATE VIEW default.v_i_3
(
    `s` UInt8,
    `col1` Int64,
    `col2` Int64,
    INDEX i_3 (col1 + col2) TYPE minmax GRANULARITY 1
) AS
SELECT 1 AS s;