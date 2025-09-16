-- schema.sql
CREATE TABLE country (
  code        TEXT PRIMARY KEY,       -- e.g., 'NO', 'SE'
  name        TEXT NOT NULL           -- e.g., 'Norway', 'Sweden'
);

CREATE TABLE city (
  geoname_id  BIGINT PRIMARY KEY,     -- e.g., 2798301
  name        TEXT NOT NULL,
  country_code TEXT NOT NULL REFERENCES country(code) ON DELETE RESTRICT,
  population  INTEGER NOT NULL,       -- 0 if missing, else as provided
  timezone    TEXT,
  latitude    DOUBLE PRECISION,
  longitude   DOUBLE PRECISION
);

-- For fast lookups by country and thresholds
CREATE INDEX idx_city_country ON city(country_code);
CREATE INDEX idx_city_country_pop ON city(country_code, population);
CREATE INDEX idx_city_pop ON city(population);

-- For the required server log (queue size vs time)
CREATE TABLE server_queue_log (
  id           BIGSERIAL PRIMARY KEY,
  server_id    TEXT NOT NULL,          -- identifier for the server in this zone (e.g., "server-1")
  ts_unix_ms   BIGINT NOT NULL,        -- timestamp in ms
  queue_length INTEGER NOT NULL
);
CREATE INDEX idx_queue_log_server_time ON server_queue_log(server_id, ts_unix_ms);