# Hexagonal Architecture

Each bounded context (`caiman-debtor`, `caiman-billing`, `caiman-payment`, `caiman-notification`) is split into three Gradle sub-modules following Ports & Adapters (hexagonal) architecture.

```
<context>/
  core/           ← the hexagon (domain + ports)
  entrypoint/     ← driving adapters (in)
  infrastructure/ ← driven adapters (out)
```

---

## `core`

The domain. Contains all business logic and defines the contracts (ports) that the outside world must satisfy.

**Allowed dependencies:** `spring-context`, `spring-tx`, `caiman-shared`. Nothing else — no web, no JPA, no mail, no external libs.

**What belongs here:**
- Domain entities and value objects
- Use case services (`@Service`) — orchestrate domain logic
- Port interfaces — inbound (`UseCase` / service interfaces called by entrypoints) and outbound (`Repository`, `Gateway`, `EmailPort`, etc. interfaces called by use cases but implemented elsewhere)
- Domain events (local, within the context — cross-context events live in `caiman-shared`)

**What does NOT belong here:** HTTP request/response types, JPA annotations (`@Entity` lives in `infrastructure`), Spring Data repositories, anything that touches I/O.

---

## `entrypoint`

Driving adapters. Code that starts an interaction with the domain — HTTP requests, scheduled jobs, event listeners.

**Allowed dependencies:** `core` of the same context.

**What belongs here:**
- REST controllers (`@RestController`) — translate HTTP ↔ use case calls
- Scheduled jobs (`@Scheduled`) — e.g. Odin (daily invoice generation), Huginn (minutely notification dispatch)
- Domain event listeners (`@EventListener`) — react to events published by other contexts

**What does NOT belong here:** business logic, database access, external API calls. Controllers call use case ports; they do not implement domain rules.

---

## `infrastructure`

Driven adapters. Implements the outbound ports defined in `core` using real I/O.

**Allowed dependencies:** `core` of the same context, `caiman-shared`.

**What belongs here:**
- JPA entities (`@Entity`) and Spring Data repositories
- Implementations of `Repository` ports (adapt JPA → port interface)
- Implementations of `Gateway` ports (e.g. `InvoiceGateway`, `DebtorGateway` — fetches cross-context data)
- External API clients (e.g. Anthropic API client for AI proof analysis)
- Email sender implementation (SMTP via Spring Mail)
- Event publisher adapter (wraps `ApplicationEventPublisher`)

**What does NOT belong here:** business logic, HTTP handling, scheduling.

---

## Dependency flow

```
entrypoint  ──►  core  ◄──  infrastructure
                  │
              (defines ports)
```

`core` is the center — it knows nothing about adapters. Adapters know about `core` but not about each other. This keeps domain logic testable in isolation and keeps infrastructure details from leaking into business rules.

---

## Cross-context communication

Bounded contexts do not import each other's Gradle modules. All cross-context contracts live in `caiman-shared`:

- **Domain events** (POJOs) — published via `ApplicationEventPublisher`, consumed by `@EventListener` in `entrypoint`
- **Gateway interfaces** — defined in `caiman-shared`, implemented in the providing context's `infrastructure`, wired by `caiman-app`

`caiman-app` is the composition root: it depends on all `:entrypoint` and `:infrastructure` modules and is the only place where cross-module wiring happens.
