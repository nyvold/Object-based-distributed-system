# Project setup and run guide

## Requirements
- Docker Desktop or Docker Engine (with Compose v2)
- ~4 GB free RAM for containers (5 Postgres, 5 servers, 1 proxy, 1 client, 5 seeders)
- Optional: set `COMPOSE_BAKE=true` for faster builds on Docker Desktop

## Quick start:

Run via CLI
- Build the project
  - `docker compose build`
- Run with NO CACHE
  - `docker compose run --rm -e JAVA_ARGS="--client-cache=false --server-cache=false" client`

- Run with SERVER cache
  - `docker compose run --rm -e SERVER_CACHE=true -e METRICS_PATH=/app/out/metrics_server_cache.csv -e JAVA_ARGS="--client-cache=false" client`

- Run WITH CLIENT cachce
  - `docker compose run --rm -e JAVA_ARGS="--client-cache=true --server-cache=false" client`

- Start in foreground (build if needed):
  - `docker compose up --build`
- Stop all services (keep volumes):
  - `docker compose down`
- Stop and remove volumes (fresh databases on next run):
  - `docker compose down -v`

## CLI flags for cache (client/server)
- Recognized flags inside the containers (passed via `JAVA_ARGS`):
  - Client:
    - `--client-cache[=true|false]`
    - `--client-cache-cap=<N>` (default 45)
  - Server:
    - `--server-cache[=true|false]`
    - `--server-cache-cap=<N>` (default 150)

## Quick ways to use flags
- Edit docker-compose.yml (already configured):
  - Servers have `JAVA_ARGS=--server-cache=true --server-cache-cap=150`.
  - Client has `JAVA_ARGS=--client-cache=true --client-cache-cap=45`.
- One-off client run with different flags (stack already running):
  1. Start proxy + servers: `docker compose up -d proxy server1 server2 server3 server4 server5`
  2. Run client once with flags: `docker compose run --rm -e JAVA_ARGS="--client-cache=true --client-cache-cap=45" client`
     (Write output to `client-output/client_cache.txt`.)
- Override file approach (toggle server cache without editing main compose):
  - Create `docker-compose.override.yml` with only the flags you want to change, e.g.:
    - `services:`
    - `  server1:`
    - `    environment:`
    - `      - JAVA_ARGS=--server-cache=false`
    - (repeat for `server2..server5` or use a YAML anchor)
  - Run with both files: `docker compose -f docker-compose.yml -f docker-compose.override.yml up --build`

## Env var fallback (alternative to flags)
- Client: `CLIENT_CACHE=true|false`, `CLIENT_CACHE_CAP=<N>`
- Server: `SERVER_CACHE=true|false`, `SERVER_CACHE_CAP=<N>`

## Output filenames by mode
- No cache: `client-output/naive_server.txt` and `metrics_naive_server.csv`
- Client cache: `client-output/client_cache.txt` and `metrics_client_cache.csv`
- Server cache: `client-output/server_cache.txt` and `metrics_server_cache.csv`

## What runs
- 1 proxy (`proxy`) on RMI 1099
- 5 servers (`server1..server5`) that register with the proxy
- 5 Postgres instances (`db1..db5`) with separate zone databases
- 5 seeders (`seed1..seed5`) importing cities dataset into each zone DB
- 1 client (`client`) that parses the input file and calls server methods through the proxy

## Caching
- Server and client both support minimal LRU caches (assignment limits):
  - Server cache: up to 150 entries (zone-local, per server)
  - Client cache: up to 45 entries
- How to enable (Compose, default in this repo):
  - Servers: cache enabled via CLI flags passed in `JAVA_ARGS` during building.
    - Each server has `JAVA_ARGS=--server-cache=${SERVER_CACHE:-true} --server-cache-cap=${SERVER_CACHE_CAP:-150}`.
    - This can be used while building using `SERVER_CACHE=false && SERVER_CACHE_CAP=20 && docker compose up --build`
    - The defualts for the environment variables is true and 150, respectivly.
  - Client: cache enabled via CLI flags passed in `JAVA_ARGS` during building.
    - Client server has: `JAVA_ARGS=--client-cache=${CLIENT_CACHE:-true} --client-cache-cap=${CLIENT_CACHE_CAP:-45}`.
    - This can be used while building using `CLIENT_CACHE=false && CLIENT_CACHE_CAP=5 && docker compose up --build`
    - The defualts for the environment variables is true and 45, respectivly.
- Output and verification:
  - Client cache hits are tagged in output lines as `Cache:CLIENT_HIT` and show `ServerZone:? WaitMs:CACHE_HIT ExecMs:CACHE_HIT`.
  - Server cache hits reduce server-side `WaitMs/ExecMs` reported back to the client (no explicit tag).
- Tuning or disabling:
  - Disable server cache: remove the `JAVA_ARGS` flag or set `--server-cache=false` (or `SERVER_CACHE=false`).
  - Disable client cache: set `--client-cache=false` (or `CLIENT_CACHE=false`).
  - Change capacities: adjust `--server-cache-cap=N` / `--client-cache-cap=N` or env vars.

## Client input and output
- Input file inside the container defaults to `/app/exercise_1_input.txt` and is bind-mounted from the repo.
- Output is written to `/app/out/naive_server.txt` and mirrored to `client-output/` on the host.
- Per-query timings appended to each output line: `WaitMs`, `ExecMs`, `TurnMs`, `TotalMs` and (on client hits) `Cache:CLIENT_HIT`.

## Dataset and seeding
- Dataset file is mounted from `Ass1-RMI/Ass1-RMI/exercise_1_dataset.csv` into each seeder.
- Seeder services run once and exit after import; the client waits for successful completion before starting.

## Notes
- If Docker cannot pull base images due to network/DNS, configure Docker Desktop DNS (for example `8.8.8.8`, `1.1.1.1`) or set proxy settings, then retry pulls:
  - `docker pull maven:3.9-eclipse-temurin-17`
  - `docker pull eclipse-temurin:17-jre`
- SLF4J binder warnings in logs are harmless.

## Developers

This program was developed by:
- kribb
- victou
- jonasbny
- jorgeteig