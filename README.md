Project setup and run guide

Requirements
- Docker Desktop or Docker Engine (with Compose v2)
- ~4 GB free RAM for containers (5 Postgres, 5 servers, 1 proxy, 1 client, 5 seeders)
- Optional: set `COMPOSE_BAKE=true` for faster builds on Docker Desktop

Quick start
- Build and start all services (proxy, servers, DBs, seeders, client):
  - `docker compose up --build`
- Follow logs:
  - `docker compose logs -f`

What runs
- 1 proxy (`proxy`) on RMI 1099
- 5 servers (`server1..server5`) that register with the proxy
- 5 Postgres instances (`db1..db5`) with separate zone databases
- 5 seeders (`seed1..seed5`) importing cities dataset into each zone DB
- 1 client (`client`) that parses the input file and calls server methods through the proxy

Dataset and seeding
- Dataset file is mounted from `Ass1-RMI/Ass1-RMI/exercise_1_dataset.csv` into each seeder.
- Seeder services run once and exit after import; the client waits for successful completion before starting.

Stop / reset
- Stop stack (keeps DB volumes):
  - `docker compose down`
- Stop and remove volumes (fresh DBs and re-seed on next up):
  - `docker compose down -v`
- Rebuild images next run:
  - `docker compose up --build`

Health checks and verification
- Check a DB has data (example zone1):
  - `docker exec -it db1 psql -U ass1 -d ass1_zone1 -c "select count(*) from city;"`
- Inspect client output (mounted):
  - `cat client-output.txt`

Notes
- If Docker cannot pull base images due to network/DNS, configure Docker Desktop DNS (for example `8.8.8.8`, `1.1.1.1`) or set proxy settings, then retry pulls:
  - `docker pull maven:3.9-eclipse-temurin-17`
  - `docker pull eclipse-temurin:17-jre`
- SLF4J binder warnings in logs are harmless.
