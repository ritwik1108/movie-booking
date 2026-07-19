# Developer & AI Collaboration Log (agents.md)

This document describes the collaboration between **Ritwik** (lead developer) and **Antigravity AI** (coding assistant) during the design, implementation, and testing of the Movie Ticket Booking System.

## Collaboration Overview

The project was built using a pair-programming methodology:
- **Ritwik** set the architectural patterns, designed the entity relationships, decided on concurrency control mechanisms, and dictated how data modifications and rollbacks behave inside transactional boundaries.
- **Antigravity AI** assisted with writing database schema migrations, Lombok entity boilerplate, DTO definitions, Spring Security filter setup, and structural integration test scaffolds.

---

## Division of Work

### 1. Boilerplate, Setup & Scaffolds (AI-Assisted)
- **Database Schema**: Assisted in translating the conceptual models into SQL tables, incorporating standard Auditing fields.
- **DTOs & JPA Entities**: Generated baseline properties, getters/setters (via Lombok annotations), and validation parameters (`@NotBlank`, `@NotNull`, etc.).
- **Security Scaffolding**: Structured the baseline JWT filtering middleware and security context configurations.

### 2. Business Logic, Transactions & Locking (Developer-Driven)
- **Pessimistic Seat Concurrency**: Ritwik directed the use of `SELECT ... FOR UPDATE` to secure seat locks. He solved the concurrent deadlock issue by requiring seat IDs to be sorted in ascending order before database locking.
- **Coupon Concurrency & Pessimistic Locks**: Serially validated coupon codes by locking coupon table rows serially during evaluations, preventing double-spend exploits.
- **Transactional Rollback States**: Ritwik designed the recovery flow for payment failures—ensuring seats are immediately freed, booking status is marked `CANCELLED`, and coupon usages are reverted in the database, while returning an HTTP 400 Bad Request to the caller.
- **Refund Calculations**: Created the dynamic matched refund rule calculation relative to show start times.

### 3. Outbox Pattern & Reminder Scheduler (Collaborative)
- **Outbox Pattern**: AI created the outbox logger template; Ritwik integrated it inside booking confirmation and cancellation transactions.
- **Asynchronous Polling**: Configured the polling scheduler to dispatch outbox items.
- **Reminder Scheduler**: Set up the show reminder sweeps to check for upcoming shows starting within 2 hours.

---

## Development History

All development was executed on temporary feature branches off the `dev` branch:
1. `feat/seat-hold` (Boilerplate entities and sorting logic)
2. `feat/expiry-sweeper` (Background sweeps and inline lazy cleanup)
3. `feat/payments-discounts` (Confirmation flow and concurrent coupon locks)
4. `feat/cancellation-refunds` (Dynamic refund calculations and cancellation)
5. `feat/outbox-notifications` (Outbox patterns and retry loops)
6. `feat/reminder-notifications` (Upcoming show reminder scheduling)

Upon successful verification, branches were merged into `dev` using a non-fast-forward strategy (`--no-ff`) to preserve commit history.
