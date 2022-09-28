SELECT s.seqrelid AS oid,
       s.seqtypid::bigint AS data_type,
       s.seqstart, 
       s.seqincrement, 
       s.seqmax, 
       s.seqmin, 
       s.seqcache, 
       s.seqcycle,
       a.attidentity 
FROM pg_catalog.pg_sequence s
LEFT JOIN pg_catalog.pg_depend d ON d.classid = 'pg_catalog.pg_class'::regclass
    AND d.objid = s.seqrelid
    AND d.objsubid = 0
    AND d.refclassid = 'pg_catalog.pg_class'::regclass
    AND d.refobjsubid != 0
    AND d.deptype IN ('i', 'a')
LEFT JOIN pg_catalog.pg_attribute a ON a.attrelid = d.refobjid
    AND a.attnum = d.refobjsubid
    AND a.attisdropped IS FALSE