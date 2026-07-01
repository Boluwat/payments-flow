# Agent Notes — Payment Platform

Multi-module Spring Boot payment system with a saga orchestrator, ledger, settlement, and API gateway.

## Build & Run

- **Build everything**: `./mvnw clean install`
- **Build one service** (e.g., payment-service): `./mvnw clean package -pl payment-service -am -DskipTests`
  - Always use `-am` so `payment-common` is built first; it is a shared library, not a bootable app.
- **Run a service locally**: `cd payment-service && ../mvnw spring-boot:run`
- **Run tests for one module**: `./mvnw test -pl payment-service -am`

## Local Dev Stack

- **PostgreSQL instances** (one per service):
  | Service | Host port | DB name |
  |---------|-----------|---------|
  | payment-service | `5432` | `PaymentService` |
  | ledger-service | `5433` | `LedgerService` |
  | settlement-service | `5434` | `SettlementService` |
- Start only a specific DB: `docker-compose up postgres-payment` (or `postgres-ledger`, `postgres-settlement`)
- **All services + observability**: `docker-compose up --build`
  - Startup order is enforced by healthchecks: all Postgres instances → ledger (8081) / settlement (8082) → payment (8080) → gateway (8090)
- **Grafana**: http://localhost:3000 (`admin`/`admin`)
  - Pre-provisioned dashboards in `infra/grafana/provisioning/dashboards/`
  - **Service Overview**: Real-time health, traffic, latency, JVM, circuit breakers, and connection pool metrics
- **Prometheus**: http://localhost:9090

## Architecture

| Module | Port | Role |
|--------|------|------|
| `gateway-service` | 8090 | Spring Cloud Gateway + Resilience4j circuit breakers |
| `payment-service` | 8080 | Saga orchestrator; calls ledger & settlement via OpenFeign |
| `ledger-service` | 8081 | Account holds, captures, releases, credits |
| `settlement-service` | 8082 | NIBSS settlement gateway (mockable) |
| `payment-common` | — | Shared DTOs, exceptions, Jackson config; **not bootable** |

### Saga Flow (payment-service)
`RESERVE_FUNDS` → `SETTLEMENT` → `CAPTURE_HOLD`. If settlement or capture fails, the orchestrator compensates (release hold; reverse settlement + post credit if needed).

### Critical Code Convention
`PaymentSagaOrchestrator.execute()` is **intentionally non-transactional**. It uses a `@Lazy` self-injected proxy to call `@Transactional` helpers (`initialize`, `transition`, `compensate`) so that no database connection is held across remote Feign calls. Do **not** add `@Transactional` to the top-level `execute` method.

## Config Quirks

- **Schema management**: `spring.jpa.hibernate.ddl-auto=update` in all services. There are no Flyway migrations despite `HELP.md` mentioning Flyway.
- **Gateway time limiter**: Raised to `30s` (default is `1s`) because the payment saga makes serial remote calls.
- **Settlement mock**: `nibss.mock.fail-rate=0.2` in `settlement-service` causes randomized failures for local testing.
- **Feign circuit breaker**: `feign.circuitbreaker.enabled=true` in `payment-service` with fallback factories (`LedgerApiFallbackFactory`, `SettlementApiFallbackFactory`).

## Endpoints & Health

- Gateway routes:
  - `/api/v1/payments/**` → payment-service
  - `/api/v1/ledger/**` → ledger-service
  - `/api/v1/settlements/**` → settlement-service
- Actuator on every service: `/actuator/health`, `/actuator/prometheus`, `/actuator/metrics`, `/actuator/info`
- All health endpoints are used by Docker Compose healthchecks.

## Testing

- Standard Spring Boot tests (`*ApplicationTests.java`). No custom testcontainers or integration-test profiles.
- Hikari leak-detection threshold is `5000ms` in `payment-service`; long-running transactions during tests will log warnings.

## Docker / Infrastructure

- **Database passwords** are kept in `db.env` and loaded via `env_file` in `docker-compose.yml`. This avoids Docker Compose `$` interpolation issues — do not put literal `$` characters in Docker Compose `environment` values.

## Idempotency

Payment initiation requires an `Idempotency-Key` header. The database has a unique constraint on `idempotency_key`. Duplicate successful requests throw `DuplicateRequestException`.
