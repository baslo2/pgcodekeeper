CREATE TABLE t1();

CREATE RULE rule_disable AS ON INSERT TO t1 DO NOTHING;
CREATE RULE rule_enable AS ON INSERT TO t1 DO NOTHING;
CREATE RULE rule_enable_replica AS ON INSERT TO t1 DO NOTHING;
CREATE RULE rule_enable_always AS ON INSERT TO t1 DO NOTHING;

ALTER TABLE t1 DISABLE RULE rule_disable;
ALTER TABLE t1 ENABLE REPLICA RULE rule_enable_replica;
ALTER TABLE t1 ENABLE ALWAYS RULE rule_enable_always;
