-- Create application role and zone-specific database if missing
DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ass1') THEN
      CREATE ROLE ass1 LOGIN PASSWORD 'ass1';
   END IF;
END
$$;

DO
$$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ass1_zone1') THEN
      CREATE DATABASE ass1_zone1 OWNER ass1;
   END IF;
END
$$;

GRANT ALL PRIVILEGES ON DATABASE ass1_zone1 TO ass1;

