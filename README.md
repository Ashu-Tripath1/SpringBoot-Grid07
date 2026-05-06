# Grid07 Virality Engine

A production-ready Spring Boot microservice that acts as the central API gateway and guardrail system for the Grid07 platform. It handles concurrent bot interactions, manages distributed state exclusively in Redis, and delivers smart batched notifications via a CRON-driven sweeper.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot 3.2 |
| Persistence | PostgreSQL 16 (via Spring Data JPA / Hibernate) |
| Cache / Guardrails | Redis 7 (via Spring Data Redis + Lettuce) |
| Scheduling | Spring `@Scheduled` |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Build | Maven 3.9 |
| Infrastructure | Docker Compose |

---

## Quick Start

### Option A — With Docker (Recommended, matches production setup)

#### 1. Start infrastructure
```bash
docker compose up -d
```
This starts PostgreSQL 16 (port 5432), Redis 7 (port 6379), and Redis Commander UI at http://localhost:8081.

#### 2. Run the application
```bash
./mvnw spring-boot:run
```

Hibernate will auto-create the schema on first boot (`ddl-auto: update`).

---

### Option B — Without Docker (Local dev, uses H2 in-memory DB)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

This uses the `application-local.yml` profile, which substitutes PostgreSQL with an H2 file-based database. Redis is still required — a Windows binary is included in the `redis/` directory:

```powershell
# Start Redis (Windows)
.\redis\redis-server.exe
```

The H2 Console is available at http://localhost:8080/h2-console  
(JDBC URL: `jdbc:h2:file:./data/grid07`, User: `sa`, Password: `password`)

---

### 3. Import the Postman collection

Import `Grid07_Virality_Engine.postman_collection.json` into Postman. Run the **"0 — Seed Data"** folder first to create a user and bot, then work through Phases 1–4 in order.

---

## Project Structure

```
src/main/java/com/grid07/
├── config/
│   ├── RedisConfig.java          # StringRedisTemplate + RedisTemplate beans
│   └── RedisKeys.java            # Central registry of all Redis key patterns
├── controller/
│   ├── PostController.java       # POST /api/posts, comments, likes
│   ├── UserController.java       # POST /api/users (seed / test)
│   └── BotController.java        # POST /api/bots (seed / test)
├── dto/                          # Request + Response DTOs with Bean Validation
├── entity/                       # JPA entities: User, Bot, Post, Comment
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── GuardrailException.java   # → HTTP 429
│   ├── ResourceNotFoundException.java # → HTTP 404
│   └── InvalidRequestException.java   # → HTTP 400
├── repository/                   # Spring Data JPA repositories
├── scheduler/
│   └── NotificationSweeper.java  # @Scheduled CRON sweeper (every 5 min)
└── service/
    ├── ViralityService.java      # Interface
    ├── GuardrailService.java     # Interface
    ├── NotificationService.java  # Interface
    ├── PostService.java          # Interface
    └── impl/
        ├── ViralityServiceImpl.java
        ├── GuardrailServiceImpl.java   # ← Core thread-safety logic
        ├── NotificationServiceImpl.java
        └── PostServiceImpl.java
```

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/users` | Create a human user |
| `POST` | `/api/bots` | Create a bot |
| `POST` | `/api/posts` | Create a post (user or bot) |
| `GET`  | `/api/posts/{id}` | Get a post with its virality score |
| `POST` | `/api/posts/{id}/comments` | Add a comment (enforces guardrails for bots) |
| `POST` | `/api/posts/{id}/like` | Human likes a post (+20 virality) |

All error responses follow the `ApiErrorResponse` envelope:

```json
{
  "status": 429,
  "error": "HORIZONTAL_CAP_EXCEEDED",
  "message": "Post 1 has already received the maximum of 100 bot replies.",
  "fieldErrors": null,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## Phase 2: Thread-Safety Deep Dive

This section explains precisely how the three Redis guardrails achieve correctness under concurrent load.

### Horizontal Cap — Atomic Lua Script

**Problem:** The naïve approach of `GET → check → INCR` has a classic time-of-check/time-of-use (TOCTOU) race condition. Two threads can concurrently read a count of `99`, both conclude the cap is not exceeded, and both increment — producing a final count of `101` and allowing two comments past the `100` limit.

**Solution:** An inline Lua script executed via Redis `EVAL`:

```lua
local current = redis.call('INCR', KEYS[1])
if current > tonumber(ARGV[1]) then
    redis.call('DECR', KEYS[1])
    return 0
end
return 1
```

Redis guarantees that a Lua script executes **atomically** — no other Redis command from any other client can interleave between the `INCR` and the comparison. The script:

1. **Increments first** — reserving a slot optimistically.
2. **Checks after** — if the new value exceeds 100, immediately decrements (rolls back the reservation) and returns `0` (rejected).
3. **Returns 1** if the slot was successfully reserved.

Under 200 concurrent threads:

```
Thread 1–100: INCR → 1..100 → all ≤ 100 → ALLOWED
Thread 101–200: INCR → 101..200 → all > 100 → DECR (rollback) → REJECTED
```

The database is **never written to** for a rejected request — the service throws `GuardrailException` before the JPA transaction is opened.

### Cooldown Cap — Atomic `SET NX`

The `SET key value NX PX ttlMs` command is a single atomic Redis operation meaning "set the key **only if it does not exist**, and give it a TTL." Spring Data Redis exposes this as:

```java
Boolean wasSet = redisTemplate.opsForValue()
    .setIfAbsent(cooldownKey, "1", Duration.ofMinutes(10));
```

- Returns `true` → key was just created → **no cooldown active** → interaction allowed, cooldown started.
- Returns `false` → key already existed → **cooldown is active** → reject with `429`.

No race condition is possible because `SET NX` is inherently atomic at the Redis level.

### Vertical Cap — Pure Integer Check

The depth level check (`depthLevel > 20`) is a stateless integer comparison that requires no Redis operation. It is performed first in the guardrail chain as it is the cheapest possible rejection.

### Guardrail Execution Order

```
enforceVerticalCap()   ← cheapest, no Redis I/O
enforceCooldownCap()   ← one atomic SET NX command
enforceHorizontalCap() ← one Lua EVAL round-trip (most expensive, last)
```

This ordering ensures the system does the least work possible on requests that will ultimately be rejected.

---

## Phase 3: Notification Engine

The notification throttler uses a two-path design:

**Immediate path** (user NOT on cooldown):
1. `SET notif_cooldown:user_{id} "1" NX PX 900000` — atomically claim the cooldown window.
2. If the key was freshly set, log `[PUSH NOTIFICATION]` to the console.

**Batching path** (user IS on cooldown):
1. `RPUSH user:{id}:pending_notifs "<notification text>"` — append to the user's pending list.

**CRON Sweeper** (every 5 minutes):
1. `KEYS user:*:pending_notifs` — discover all pending lists (uses `SCAN` semantics via Spring Data Redis for production safety).
2. For each list: `LRANGE key 0 -1` to read all messages, then `DEL key` to atomically clear it.
3. Constructs a summary: `"Bot Alpha and [N] others interacted with your posts."` and logs it.

---

## Redis Key Schema

| Key | Type | TTL | Description |
|---|---|---|---|
| `post:{id}:virality_score` | String (int) | None | Running virality score |
| `post:{id}:bot_count` | String (int) | None | Total bot replies on a post |
| `cooldown:bot_{id}:human_{id}` | String | 10 min | Bot–human interaction cooldown |
| `notif_cooldown:user_{id}` | String | 15 min | Notification throttle sentinel |
| `user:{id}:pending_notifs` | List | None | Batched notification queue |

---

## Statelessness

The application is **completely stateless**. There are zero `static` variables, no `HashMap` or `ConcurrentHashMap` fields used as caches, and no `@ApplicationScope` beans holding mutable state. All counters, cooldowns, and queues live exclusively in Redis. Any number of application instances can run behind a load balancer without coordination.
