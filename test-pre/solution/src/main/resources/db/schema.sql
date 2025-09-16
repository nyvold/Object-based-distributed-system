-- schema.sql
CREATE TABLE IF NOT EXISTS country (
  code        TEXT PRIMARY KEY,       -- e.g., 'NO', 'SE'
  name        TEXT NOT NULL           -- e.g., 'Norway', 'Sweden'
);

CREATE TABLE IF NOT EXISTS city (
  geoname_id  BIGINT PRIMARY KEY,     -- e.g., 2798301
  name        TEXT NOT NULL,
  country_code TEXT NOT NULL REFERENCES country(code) ON DELETE RESTRICT,
  population  INTEGER NOT NULL,       -- 0 if missing, else as provided
  timezone    TEXT,
  latitude    DOUBLE PRECISION,
  longitude   DOUBLE PRECISION
);

-- For fast lookups by country and thresholds
CREATE INDEX IF NOT EXISTS idx_city_country ON city(country_code);
CREATE INDEX IF NOT EXISTS idx_city_country_pop ON city(country_code, population);
CREATE INDEX IF NOT EXISTS idx_city_pop ON city(population);

-- For the required server log (queue size vs time)
CREATE TABLE IF NOT EXISTS server_queue_log (
  id           BIGSERIAL PRIMARY KEY,
  server_id    TEXT NOT NULL,          -- identifier for the server in this zone (e.g., "server-1")
  ts_unix_ms   BIGINT NOT NULL,        -- timestamp in ms
  queue_length INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_queue_log_server_time ON server_queue_log(server_id, ts_unix_ms);

-- Ensure app user can read (harmless if user doesn't exist)
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ass1') THEN
    GRANT SELECT ON ALL TABLES IN SCHEMA public TO ass1;
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ass1;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ass1;
  END IF;
END $$;
