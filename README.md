# ShortLink

**A production-hardened URL shortener** — built as a follow-up portfolio project to [GatewayX](https://github.com/aroravivek398/gatewayx), applying the same engineering rigor (testing, Docker, AWS deployment, async processing) to a genuinely different domain, with a full React frontend on top.

🔗 **Live app:** https://url-shortener-rose-theta.vercel.app
🔗 **Live API:** https://13.127.199.47.sslip.io

> **Note:** the backend runs on a personal AWS EC2 instance that is stopped when not actively in use, to manage cloud costs. If the live links above aren't responding, the instance is likely stopped — reach out and I'll spin it back up, or see the [Running Locally](#running-locally) section below to run it yourself.

---

## What ShortLink Does

Register, create short links (with optional custom aliases and expiration dates), and track how they're used — click counts, device breakdown (mobile/desktop/tablet), and scannable QR codes for every link. Built to demonstrate the same distributed-systems fundamentals as GatewayX (caching, async processing, atomicity), applied to a different, genuinely common product.

---

## Core Features

### Authentication & Ownership
- JWT-based login, BCrypt password hashing
- Every URL, delete action, and analytics view is scoped to its owner — verified server-side, not just hidden in the UI

### Collision-Free Short Codes
- Short codes are generated via **Base62 encoding of the database's own auto-incrementing ID** — the same technique real URL shorteners (bit.ly, TinyURL) use
- This guarantees uniqueness by construction, with zero collision-checking overhead — no random-string-and-retry logic needed
- Custom aliases are also supported, with an explicit uniqueness check for that specific case

### Redis Caching
- Redirects are served via a cache-aside pattern: check Redis first, fall back to the database on a miss, populate the cache for next time
- Verified correct with a Testcontainers test that deletes the underlying database row *after* caching it, then confirms the redirect still succeeds — proving the response genuinely came from cache, not a lucky database hit

### Async Click Tracking
- Every redirect publishes a Kafka event rather than writing to the database synchronously, keeping the redirect itself fast
- A consumer processes these events to update click counts and store per-click metadata (IP, user agent) for analytics — decoupled from the user-facing request, same pattern used for usage metering in GatewayX

### Link Expiration
- Optional expiry date per link; expired links are rejected with `410 Gone`, checked correctly on both cache hits and cache misses (the cache stores expiry alongside the URL specifically to avoid serving expired links from a stale cache entry)

### QR Codes
- Every short link has an auto-generated, downloadable QR code (via ZXing), publicly accessible without authentication — since a QR code's entire purpose is to be shared

### Device Analytics
- Click metadata is parsed into a mobile/desktop/tablet breakdown, viewable per link

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot |
| Database | MySQL |
| Caching | Redis |
| Async Messaging | Apache Kafka |
| Auth | JWT, BCrypt |
| Frontend | React (Vite), Tailwind CSS |
| Containerization | Docker, Docker Compose |
| Cloud | AWS EC2, Nginx (reverse proxy), Let's Encrypt (SSL) |
| Frontend Hosting | Vercel |
| Testing | JUnit 5, Mockito, Testcontainers |

---

## Architecture

```
                          ┌──────────────┐
   React (Vercel)  ─────▶ │ Nginx (HTTPS)│ ──▶ Spring Boot App
                          └──────────────┘         │
                                                    │
                       ┌────────────────────────────┼────────────────────┐
                       ▼                            ▼                    ▼
                  Redis (cache)              MySQL (data)         Kafka (async)
                  shortUrl → originalUrl                                │
                  + expiryDate                                         ▼
                                                              Click count + analytics
                                                              updated asynchronously
```

---

## Running Locally

### Prerequisites
- Docker and Docker Compose
- Node.js (for the frontend)

### Backend

```bash
git clone https://github.com/aroravivek398/url-shortener.git
cd url-shortener
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties` with your local values, then:

```bash
docker compose up -d
```

This starts MySQL, Redis, Kafka, Zookeeper, and the app itself. Give it 30-40 seconds — Kafka takes a moment to stabilize.

### Frontend

```bash
cd url-shortener-frontend
npm install
echo "VITE_API_URL=http://localhost:8080" > .env
npm run dev
```

Visit `http://localhost:5173`.

---

## Testing

```bash
mvn test
```

Includes pure-function tests (Base62 encoding, device classification), Mockito-based service tests (ownership checks, custom alias collision handling), and a Testcontainers-based integration test that verifies the Redis caching layer is genuinely being used — not just configured.

---

## Deployment Notes

- Backend runs on AWS EC2 via Docker Compose, behind an Nginx reverse proxy
- HTTPS via a free [sslip.io](https://sslip.io) hostname (maps a hostname directly to the server's IP with no DNS setup required) and a Let's Encrypt certificate via Certbot, with automatic renewal configured
- Frontend deployed separately on Vercel, communicating with the backend over HTTPS — CORS is configured with a wildcard pattern (`https://*.vercel.app`) to correctly allow Vercel's per-deployment preview URLs, not just the main production URL

---

## Notes on Scope

This project was deliberately built as a companion to GatewayX — same rigor (real bug fixes, real testing, real deployment), different domain and different stack details (MySQL instead of PostgreSQL, a full React frontend instead of an API-only service) to demonstrate range rather than repeating the same patterns twice.