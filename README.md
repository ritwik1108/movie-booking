# Movie Ticket Booking System

A high-performance, single-instance, concurrency-safe REST API for booking movie tickets. Built using **Java 21 + Spring Boot 3.x + Spring Data JPA + Flyway + H2 (with Postgres compatibility mode) / PostgreSQL**.

## Features & Implementation Highlights
- **Pessimistic Concurrency Control**: Uses database-level row locks (`SELECT ... FOR UPDATE`) in a single transaction with strictly ordered seat IDs to prevent deadlocks and double-booking.
- **Role-Based Access Control**: Standard username/password auth generating stateless JWT tokens, enforcing access control on Admin vs. Customer endpoints.
- **Dynamic Pricing Engine**: Computes seat prices based on base seat tiers (REGULAR, PREMIUM, RECLINER) cascading from Show scope -> Theater scope -> Global scope. Automatically applies weekend multipliers at calculation time.
- **Self-Healing Expiry Sweep**: Expired seat holds are released automatically through both a scheduled sweeper thread and an inline lazy-check whenever a user queries seat layouts.
- **Transactional Outbox pattern**: Persists pending notifications in the database in the same transaction as bookings, dispatching them asynchronously to satisfy "non-blocking" rules without external message queues.

---

## Technical Stack
- **Framework**: Spring Boot 3.3.1
- **Language**: Java 21
- **Database**: PostgreSQL (Production) / H2 in Postgres-compatible mode (Default / Local / Testing)
- **Migrations**: Flyway
- **Security**: Spring Security + JJWT (JSON Web Token)
- **API Docs**: Springdoc-OpenAPI (Swagger UI)

---

## Logged Assumptions

1. **Authentication**: Simple username/password authentication issuing JWTs is sufficient. No OAuth/SSO/MFA.
2. **Database Fallback**: Defaults to an in-memory H2 database using `PostgreSQL` compatibility mode so the application runs out of the box without any external Docker/Postgres daemon setup. Supports PostgreSQL out of the box via active environment variables.
3. **Pessimistic Locking**: `READ_COMMITTED` transaction isolation + `SELECT ... FOR UPDATE` ordered by seat ID is utilized to serialize requests and prevent concurrent double-holds.
4. **Weekend pricing**: Any show whose date falls on Saturday/Sunday; multiplier is globally or theater/show configurable.
5. **Seat holds**: Expire in 5 minutes (configurable in `application.yml` via `app.hold.ttl-minutes`).
6. **Outbox Poller**: In-process scheduler polling the database every 5 seconds (configurable) and executing outbox dispatches asynchronously.
7. **Refunding**: Policies are defined dynamically via Admin endpoints. Cancellation uses the cancel request timestamp relative to the show's start time to calculate the percentage refund.

---

## Getting Started

### Prerequisites
- **Java**: JDK 21+

### Running the Application
To run the application locally:
```bash
mvn spring-boot:run
```

*Note: If you do not have Maven installed globally, you can invoke your local portable Maven path:*
```powershell
& C:\Users\Ritwik\.gemini\antigravity\scratch\maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

Once running, you can access the Swagger UI explorer:
- **API Explorer**: `http://localhost:8080/swagger-ui/index.html`

### Default Seed Users
The system comes pre-seeded with two accounts for testing:
- **Admin**: `admin@movie.com` / `admin123`
- **Customer**: `customer@movie.com` / `customer123`

---

## Running Tests
To run unit and integration tests:
```bash
mvn clean test
```

*Note: If you do not have Maven installed globally, run via your local portable Maven:*
```powershell
& C:\Users\Ritwik\.gemini\antigravity\scratch\maven\apache-maven-3.9.6\bin\mvn.cmd clean test
```
