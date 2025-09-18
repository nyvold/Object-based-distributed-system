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

Caching
- Server and client both support minimal LRU caches (assignment limits):
  - Server cache: up to 150 entries (zone-local, per server)
  - Client cache: up to 45 entries
- How to enable (Compose, default in this repo):
  - Servers: cache enabled via CLI flags passed in `JAVA_ARGS`.
    - See `docker-compose.yml:115`, `docker-compose.yml:157`, `docker-compose.yml:199`, `docker-compose.yml:222`, `docker-compose.yml:264`.
      Each server has `JAVA_ARGS=--server-cache=true --server-cache-cap=150`.
  - Client: cache enabled via CLI flags passed in `JAVA_ARGS`.
    - See `docker-compose.yml:106` (`client` service): `JAVA_ARGS=--client-cache=true --client-cache-cap=45`.
- Alternative (env fallback if you prefer not to use CLI flags):
  - Server: set `SERVER_CACHE=true` and optional `SERVER_CACHE_CAP=150`.
  - Client: set `CLIENT_CACHE=true` and optional `CLIENT_CACHE_CAP=45`.
- Output and verification:
  - Client cache hits are tagged in output lines as `Cache:CLIENT_HIT` and show `ServerZone:? WaitMs:CACHE_HIT ExecMs:CACHE_HIT`.
  - Server cache hits reduce server-side `WaitMs/ExecMs` reported back to the client (no explicit tag).
- Tuning or disabling:
  - Disable server cache: remove the `JAVA_ARGS` flag or set `--server-cache=false` (or `SERVER_CACHE=false`).
  - Disable client cache: set `--client-cache=false` (or `CLIENT_CACHE=false`).
  - Change capacities: adjust `--server-cache-cap=N` / `--client-cache-cap=N` or env vars.

Client input and output
- Input file inside the container defaults to `/app/exercise_1_input.txt` and is bind-mounted from the repo.
- Output is written to `/app/out/naive_server.txt` and mirrored to `client-output/` on the host.
- Per-query timings appended to each output line: `WaitMs`, `ExecMs`, `TurnMs`, `TotalMs` and (on client hits) `Cache:CLIENT_HIT`.

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
  - `cat client-output/naive_server.txt`
  - Optional CSV metrics (same folder): `client-output/metrics.csv`

Notes
- If Docker cannot pull base images due to network/DNS, configure Docker Desktop DNS (for example `8.8.8.8`, `1.1.1.1`) or set proxy settings, then retry pulls:
  - `docker pull maven:3.9-eclipse-temurin-17`
  - `docker pull eclipse-temurin:17-jre`
- SLF4J binder warnings in logs are harmless.
