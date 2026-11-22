# 04 — Docker & running the stack

## One command
```bash
docker compose up -d --build
```
| Container | Image | Port | Depends on |
|---|---|---|---|
| walkthrough-postgres | postgres:16-alpine | 5432 | — |
| walkthrough-pgadmin | dpage/pgadmin4 | 5050 | postgres healthy |
| walkthrough-backend | built from `backend/Dockerfile` | 8080 | postgres healthy |
| walkthrough-frontend | built from `frontend/Dockerfile` | 4200 → 80 | backend healthy |

Start order is enforced with **healthchecks** + `depends_on: condition: service_healthy`:
Postgres answers `pg_isready` → backend starts, runs Flyway, reports `/actuator/health` = UP → frontend starts.

## Multi-stage Dockerfiles
- **Backend**: `maven:...-temurin-21` compiles the jar → copied into a small `eclipse-temurin:21-jre-alpine`,
  runs as a non-root user. `pom.xml` is copied first so dependency downloads are cached.
- **Frontend**: `node:20-alpine` runs `npm ci && ng build` → static files copied into `nginx:alpine`.
  `nginx.conf` serves the SPA and proxies `/api` to `http://backend:8080` (service name = hostname).

## Daily commands
```bash
docker compose ps                         # health of each service
docker compose logs -f backend            # Spring Boot logs
docker compose up -d --build backend      # rebuild only the API after Java changes
docker compose up -d --build frontend     # rebuild only the UI
docker compose down                       # stop, keep data
docker compose down -v                    # stop and wipe DB volume (seed runs again)
```

## Running pieces locally (faster feedback)
- **DB only**: `docker compose up -d postgres pgadmin`
- **Backend from IntelliJ**: run `WalkthroughApiApplication` — defaults in `application.yml` point to `localhost:5432`.
- **Frontend with hot reload**: `cd frontend && npm install && npm start` → http://localhost:4200
  (`proxy.conf.json` forwards `/api` to `localhost:8080`). Stop the frontend container first
  (`docker compose stop frontend`) since both use port 4200.

## Backend tests without installing Maven
```bash
docker run --rm -v "${PWD}/backend:/workspace" -v walkthrough-m2:/root/.m2 -w /workspace \
  maven:3.9.9-eclipse-temurin-21 mvn -B test
```
