-- Create application role if missing (allowed in transaction)
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ass1') THEN
      CREATE ROLE ass1 LOGIN PASSWORD 'ass1';
   END IF;
END $$;

-- Create the zone database if missing (not allowed in DO; use \gexec)
SELECT 'CREATE DATABASE ass1_zone2 OWNER ass1'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ass1_zone2')\gexec

-- Ensure permissions if DB exists already
GRANT ALL PRIVILEGES ON DATABASE ass1_zone2 TO ass1;
