# AI-Assisted Emergency Radiology Reporting Assistant

[![CI](https://github.com/tugbasozuer/radyoloji-raporlama-asistani/actions/workflows/ci.yml/badge.svg)](https://github.com/tugbasozuer/radyoloji-raporlama-asistani/actions/workflows/ci.yml)

A web application that helps radiologists write **emergency radiology reports** faster. The radiologist types short, structured notes; the system expands them into a complete report (*Findings* and *Impression / Recommendations*), and the radiologist reviews, edits and approves the result. Nothing becomes final without the radiologist's approval (human-in-the-loop).

> **Academic project.** Developed as a capstone project for the Hacettepe University Information Systems (non-thesis master's) program. It uses **synthetic data only** and is **not a medical device**; it must not be used for real clinical decisions.

The user interface and domain terms (e.g. *Bulgular*, *Sonuç / Öneriler*, *TASLAK / ONAYLANDI*) are in Turkish; code, comments and this README are in English.

## How it works

1. A radiologist selects a patient and opens a new report for an exam (CT, MRI, X-ray).
2. They type short notes under fixed headings (abnormal findings, "not observed", "not detected", ...) and a free-text result / recommendation.
3. On save, the system generates report sentences from fixed, deterministic sentence patterns (**template path**, no AI call).
4. On demand ("YZ ile Üret" button) a language model rewrites the notes into fluent report text and classifies the result into one of ten approved result categories. Each category adds a standard recommendation sentence and sets the urgency level. If the model's answer is invalid or the call fails, the system falls back to the radiologist's own text.
5. The radiologist edits and approves the report. Approved reports are immutable; corrections create a new draft linked to the original.

The model never writes free clinical conclusions on its own: classification is constrained to a fixed category list, and the standard recommendation sentences come from the database.

## Tech stack

| Area | Technology |
| --- | --- |
| Language / build | Java 21, Maven |
| Framework | Spring Boot 4.1 (Spring Framework 7) |
| Database | PostgreSQL 18, Spring Data JPA / Hibernate, Flyway migrations |
| Security | Spring Security, JWT (Bearer header or HttpOnly cookie) |
| Cache / rate limit | Redis 7 (JWT revocation on logout, per-user rate limit on AI calls) |
| AI | Spring AI with Anthropic (`claude-haiku-5-5`) |
| UI | Thymeleaf (server-side rendering) + small vanilla JavaScript |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc (`@WebMvcTest`), Testcontainers |
| Packaging | Docker, Docker Compose |

## Architecture

```
Browser ──► Thymeleaf pages (web)   ┐
REST client ─► REST API (controller)┴─► Services ─► Repositories ─► PostgreSQL
                                           │
                                           ├─► Redis (token blacklist, rate limit)
                                           └─► AI report generator ─► Anthropic API
```

Packages under `com.tugba.radyoloji_raporlama_asistani`: `entity`, `repository`, `dto`, `service`, `controller` (REST, JSON errors as `ProblemDetail`), `web` (Thymeleaf pages), `security`, `ai`, `exception`, `config`.

## Getting started

### Run everything with Docker Compose

Requirements: Docker with Compose.

```bash
cp .env.example .env
# Edit .env: set DB_PASSWORD and a random JWT_SECRET (at least 32 characters), e.g.
#   openssl rand -base64 48

# Optional: enable AI generation (otherwise the "YZ ile Üret" button reports that no key is configured)
export ANTHROPIC_API_KEY=your-key

docker compose up --build
```

Open <http://localhost:8080>, register a user at `/kayit`, then sign in. Four synthetic demo patients are seeded by the database migrations. Swagger UI is available at <http://localhost:8080/swagger-ui.html>.

Stop with `docker compose down` (data volumes are kept).

### Run the application locally (development)

Requirements: JDK 21 and Docker.

```bash
docker compose up -d postgres redis
export ANTHROPIC_API_KEY=your-key   # optional
./mvnw spring-boot:run
```

The default configuration in `application.properties` matches this setup (PostgreSQL on port 5433, Redis on 6379).

### Configuration

All environment-specific values are read from environment variables, with local-development defaults:

| Variable | Purpose |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection |
| `JWT_SECRET` | JWT signing key (at least 32 characters) |
| `JWT_COOKIE_SECURE` | Send the auth cookie over HTTPS only (`true` behind HTTPS) |
| `ANTHROPIC_API_KEY` | Anthropic API key (never commit it) |

The AI rate limit (default 5 requests per 60 seconds per user) is set with `ratelimit.ai.max-requests` and `ratelimit.ai.window-seconds`.

## Tests

```bash
./mvnw test
```

191 automated tests: unit tests (services, security, report generation), web-layer tests for REST and Thymeleaf controllers, and a Testcontainers-based integration test that runs the Flyway migrations and repository queries against a real PostgreSQL instance. Docker must be running for the integration test.

## Project structure

```
src/main/java/...        application code (see Architecture)
src/main/resources/
  db/migration/          Flyway migrations (schema + seed data)
  templates/, static/    Thymeleaf templates and static assets
src/test/java/...        tests
mockup/                  static HTML/CSS mockup used as the UI design source
Dockerfile               multi-stage build (Maven build, JRE runtime)
docker-compose.yml       PostgreSQL, Redis and the application
```

## Limitations

- Synthetic data only; no clinical validation and no integration with RIS/PACS/HIS systems.
- Sentence generation and result categories target Turkish emergency radiology reports.
- Not intended for production or clinical use.
