\connect ass1_zone5
SET ROLE ass1;
CREATE TABLE IF NOT EXISTS country (
  code        TEXT PRIMARY KEY,
  name        TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS city (
  geoname_id  BIGINT PRIMARY KEY,
  name        TEXT NOT NULL,
  country_code TEXT NOT NULL REFERENCES country(code) ON DELETE RESTRICT,
  population  INTEGER NOT NULL,
  timezone    TEXT,
  latitude    DOUBLE PRECISION,
  longitude   DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_city_country ON city(country_code);
CREATE INDEX IF NOT EXISTS idx_city_country_pop ON city(country_code, population);
CREATE INDEX IF NOT EXISTS idx_city_pop ON city(population);

CREATE TABLE IF NOT EXISTS server_queue_log (
  id           BIGSERIAL PRIMARY KEY,
  server_id    TEXT NOT NULL,
  ts_unix_ms   BIGINT NOT NULL,
  queue_length INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_queue_log_server_time ON server_queue_log(server_id, ts_unix_ms);
GRANT SELECT ON ALL TABLES IN SCHEMA public TO ass1;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ass1;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ass1;
RESET ROLE;
