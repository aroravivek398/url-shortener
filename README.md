
# ShortLink

**A production-grade URL shortener** — built as a backend engineering portfolio project demonstrating caching, async event-driven processing, and reliable deployment practices on top of a classic CRUD use case.

🔗 **Live demo (frontend):** https://url-shortener-rose-theta.vercel.app


> **Note:** the backend runs on a cost-optimized AWS EC2 instance that is kept **stopped by default** and started on demand (before a demo/interview) to avoid 24/7 billing. If the live demo link isn't responding, the backend is most likely stopped — see [Live Deployment](#live-deployment) below, or run it locally with the steps in [Getting Started](#getting-started).

---

## What ShortLink Does

ShortLink takes a long URL and returns a short, shareable one — but built with the same rigor as a real production system rather than a tutorial project:

- **Authenticated** — JWT-based login (BCrypt password hashing), every URL is owned by a user
- **Custom aliases** — users can optionally pick their own short code, checked for availability before it's accepted
- **Base62-encoded short codes** — generated from the auto-incrementing database ID when no custom alias is given
- **Cached redirects** — the `shortUrl → originalUrl` mapping is cached in Redis for fast lookups, independent of click tracking
- **Async click tracking** — every redirect publishes a click event to Kafka; a consumer updates click counts and device analytics without blocking the redirect response
- **QR codes** — any short link can be rendered as a scannable QR code
- **Analytics** — per-URL click counts over a date range, plus device/browser breakdown
- **Expiring links** — URLs can expire; redirects to an expired link are rejected
- **Ownership checks** — a user can only view, analyze, or delete their own URLs

---

## Core Features

### Authentication & Users
- Registration and JWT-based login (BCrypt password hashing)
- Role-based access control (`ROLE_USER`)
- Stateless JWT filter chain (Spring Security)

### URL Shortening
- Base62 encoding of the auto-incrementing entity ID (compact, collision-free short codes)
- Optional custom alias, validated for availability before creation
- Per-user URL list (`GET /api/urls/myurls`)
- Soft deletion with ownership enforcement (403 on violation, not just 401)

### Caching
- Redis caches the `shortUrl → originalUrl` mapping only — not the full entity or click count — so redirect lookups stay fast and cheap
- Click-count updates are decoupled from the cache, avoiding cache invalidation churn on every click

### Async Click Tracking (Kafka)
- Every redirect publishes a `ClickEvent` (timestamp, device/browser info) to Kafka instead of writing synchronously on the hot path
- A consumer persists click events and aggregates them for analytics — so a slow DB write never slows down the actual redirect

### Analytics
- Click counts grouped by date range
- Device/browser classification per URL (`GET /api/urls/analytics/{shortUrl}/devices`)

### QR Codes
- `GET /api/urls/{shortUrl}/qrcode` returns a PNG QR code encoding the full short link

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Framework | Java 21, Spring Boot |
| Database | MySQL 8 |
| Caching | Redis |
| Async Messaging | Apache Kafka (+ Zookeeper) |
| Auth | JWT (JJWT), BCrypt |
| Frontend | React (Vite) + Tailwind CSS |
| Containerization | Docker, Docker Compose |
| CI | GitHub Actions (backend tests + Docker build, frontend build) |
| Cloud | AWS EC2 (backend), Vercel (frontend) |
| HTTPS | Nginx reverse proxy + Let's Encrypt (Certbot) |
| Testing | JUnit 5, Mockito, Testcontainers (MySQL) |
| API Documentation | SpringDoc OpenAPI (Swagger UI) |

---

## Architecture

```
                     ┌──────────────┐
  Client Request ──▶ │ JWT Filter   │
                     └──────┬───────┘
                            │
                     ┌──────▼───────┐
                     │  Controller  │
                     └──────┬───────┘
                            │
              ┌─────────────┼──────────────┐
              ▼                            ▼
      ┌───────────────┐            ┌───────────────┐
      │ Redis Cache   │            │ Click Event   │──▶ Kafka ──▶ Consumer
      │ shortUrl →    │            │ Producer      │              │
      │ originalUrl   │            └───────────────┘              ▼
      └───────────────┘                                   click_events +
              │                                            device analytics
              ▼                                                (MySQL)
         Redirect (302)
```

Frontend (React/Vite) is deployed separately on Vercel and talks to the backend over HTTPS.

---

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/public/register` | Register a new user |
| POST | `/api/auth/public/login` | Log in, receive a JWT |
| POST | `/api/urls/shorten` | Create a short URL (auth required) |
| GET | `/api/urls/myurls` | List the authenticated user's URLs |
| GET | `/{shortUrl}` | Redirect to the original URL |
| GET | `/api/urls/{shortUrl}/qrcode` | Get a QR code (PNG) for a short URL |
| GET | `/api/urls/analytics/{shortUrl}` | Click events for a URL in a date range |
| GET | `/api/urls/analytics/{shortUrl}/devices` | Device/browser breakdown for a URL |
| GET | `/api/urls/totalClicks` | Total clicks by date for the authenticated user |
| DELETE | `/api/urls/{shortUrl}` | Delete a URL (owner only) |

Full interactive documentation is available via Swagger UI once the app is running.

---

## Live Deployment

The backend runs on an AWS EC2 instance behind an Nginx reverse proxy with a free Let's Encrypt (via sslip.io) TLS certificate, so the API is served over HTTPS without needing a purchased domain. The instance is **stopped when not in active use** — it's started manually before a demo and stopped afterward, which is a deliberate cost decision for a portfolio project rather than an always-on service.

If the live demo isn't responding, the backend instance is likely stopped — run the project locally instead (below), or reach out and I can start it.

---

## Getting Started (Local)

### Prerequisites
- Java 21
- Docker & Docker Compose
- Node.js 20+ (for the frontend)

### Backend
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
# fill in your local DB credentials and JWT secret

docker compose up -d
```
The API will be available at `http://localhost:8081`, with Swagger UI at `http://localhost:8081/swagger-ui/index.html`.

### Frontend
```bash
cd url-shortener-frontend
npm install
npm run dev
```

---

## Testing

```bash
./mvnw test
```
Service-layer tests use Mockito; repository/caching behavior is verified against a real MySQL instance via Testcontainers.
