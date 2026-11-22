# Java 21 Walkthrough

Full-stack learning project: **Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Angular 17 · Angular Material · Bootstrap utilities · Docker**.

## Quick start
```bash
cp .env.example .env          # optional, defaults work without it
docker compose up -d --build
```
| What | URL |
|---|---|
| Dashboard (Angular) | http://localhost:4200 |
| Transactions (upload PhonePe CSV) | http://localhost:4200/transactions |
| Summary API | http://localhost:8080/api/v1/summary |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| pgAdmin | http://localhost:5050 (DB password: `walkthrough`) |

## Learn
Start with [docs/00-roadmap.md](docs/00-roadmap.md).

## For AI assistants / picking up work
[CLAUDE.md](CLAUDE.md) (project guide) → [MEMORY.md](MEMORY.md) (status & next steps) → [AGENT.md](AGENT.md) (rules).
