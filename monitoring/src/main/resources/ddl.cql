CREATE KEYSPACE IF NOT EXISTS akkaviz WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE IF NOT EXISTS akkaviz.received_record (
  id TIMEUUID,
  millis BIGINT,
  first TEXT,
  direction TEXT,
  second TEXT,
  data TEXT,
  PRIMARY KEY (first, direction, second, id)
);