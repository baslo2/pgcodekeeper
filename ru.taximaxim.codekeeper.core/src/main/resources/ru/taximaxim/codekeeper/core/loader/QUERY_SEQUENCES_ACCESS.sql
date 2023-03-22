SELECT s.qname,  pg_catalog.has_sequence_privilege(s.qname, 'SELECT') AS has_priv    
FROM ( SELECT pg_catalog.unnest(?) ) s(qname)