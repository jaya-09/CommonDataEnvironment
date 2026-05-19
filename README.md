# CDE Platform

Common Data Environment — 4 microservices + React UI, fully containerised.

## Prerequisites

- **Docker Desktop** (or Docker Engine + Compose plugin)
- That's it. No Java, Maven, or Node needed locally.

## Start Everything

```bash
# Clone / unzip the project, then:
cd cde-platform

docker compose up --build
```

First run downloads base images and compiles all services inside Docker (~5–10 min).
Subsequent runs use layer cache and start in ~30 seconds.

## URLs

| Service            | URL                              |
|--------------------|----------------------------------|
| **React UI**       | http://localhost:3000            |
| API Gateway        | http://localhost:8080            |
| PLM Service        | http://localhost:8081/swagger-ui.html |
| QLM Service        | http://localhost:8082/swagger-ui.html |
| LLM Service        | http://localhost:8083/swagger-ui.html |
| Analytics Service  | http://localhost:8084/swagger-ui.html |
| Pub/Sub Emulator   | http://localhost:8085            |

## Architecture

```
React UI (3000)
    │
    ▼
API Gateway (8080)
    ├── /api/v1/plm/** → PLM Service (8081) → plm_db (5432)
    ├── /api/v1/qlm/** → QLM Service (8082) → qlm_db (5433)
    ├── /api/v1/llm/** → LLM Service (8083) → llm_db (5434)
    └── /api/v1/analytics/** → Analytics (8084) → analytics_db (5435)

All services publish events to Google Pub/Sub emulator (8085).
Analytics service subscribes to ALL topics and builds cross-service read models.
```

## Useful Commands

```bash
# View logs for a specific service
docker compose logs -f plm-service

# Restart a single service after code changes
docker compose up --build plm-service

# Stop everything (keeps data volumes)
docker compose down

# Stop and wipe all data (fresh start)
docker compose down -v

# Check running containers
docker compose ps
```

## Services

| Container | Image | Purpose |
|---|---|---|
| cde-plm-db | postgres:16-alpine | PLM database |
| cde-qlm-db | postgres:16-alpine | QLM database |
| cde-llm-db | postgres:16-alpine | LLM database |
| cde-analytics-db | postgres:16-alpine | Analytics read model DB |
| cde-redis | redis:7-alpine | Distributed cache |
| cde-pubsub-emulator | google-cloud-cli:emulators | Local Pub/Sub |
| cde-pubsub-setup | google-cloud-cli:emulators | Creates topics/subscriptions (exits after) |
| cde-llm-service | Built from llm-service/ | User & certification management |
| cde-plm-service | Built from plm-service/ | Product lifecycle management |
| cde-qlm-service | Built from qlm-service/ | Quality management |
| cde-analytics-service | Built from analytics-service/ | Cross-service insights |
| cde-api-gateway | Built from api-gateway/ | Single entry point, rate limiting |
| cde-frontend | Built from frontend/ | React UI served by nginx |
