Infra layout for per-instance Postgres databases

This folder contains everything a Dockerfile or Compose setup can reference to run one Postgres database per server instance (zone1–zone5). No Dockerfile is provided here per request.

How to use with Docker/Compose (example outline)
- Build/run Postgres using the official `postgres` image per zone.
- Provide `--env-file infra/db/zoneN/db.env` to initialize the container superuser.
- Mount `infra/db/zoneN/init` to `/docker-entrypoint-initdb.d` to create the app role and per-zone database.
- Optionally bind-mount `infra/db/zoneN/data` to `/var/lib/postgresql/data` for local persistence, or use a named Docker volume instead.
- The application for zone N should read environment from `infra/db/zoneN/app.env`.

Folder map
- infra/db/zoneN/app.env: App env consumed by the server (moved from root `.env.zoneN`).
- infra/db/zoneN/db.env: Container env for the Postgres image (superuser + default DB).
- infra/db/zoneN/init/*.sql: Idempotent SQL to create the app role and the per-zone DB.
- infra/db/zoneN/data/: Optional local persistence mount point (empty by default).

Notes
- `db.env` sets a privileged superuser (`postgres`) used only during first boot; the init scripts then create the application role `ass1` and the zone-specific database.
- The JDBC URLs in `app.env` still point to `localhost`. In containerized deployments, point them to your DB service hostname (e.g., `jdbc:postgresql://db-zone1:5432/ass1_zone1`).

