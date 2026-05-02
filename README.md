# Nordic Dev Mentor

A Spring Boot middleware that proxies chat requests to OpenRouter with four
distinct mentor personalities, paired with a Next.js frontend in editorial
Nordic design.

**Live demo:** https://frontend-production-25e3.up.railway.app/

## What it does

Each mentor has its own system prompt and sampling temperature, giving them
distinct voices:

| Mentor | Role |
|---|---|
| `junior-helper` | Patient mentor for beginners — explains from first principles |
| `senior-architect` | "It depends" — focuses on trade-offs and judgment calls |
| `code-reviewer` | Strict and direct, no sugarcoating |
| `rubber-duck` | Asks Socratic questions instead of answering, with a programming joke at the end |

Conversations persist via localStorage. The History page lets you reopen
prior sessions or delete them. Personality can be switched mid-conversation
without losing context.

## Tech stack

**Backend**
- Spring Boot 4 on Java 21
- Spring WebClient against OpenRouter
- Reactor `Retry.backoff` (429 / 5xx with exponential delay + Idempotency-Key for dedup)
- springdoc-openapi at `/swagger-ui.html`
- In-memory conversation store with sliding-window history (10 messages)
- Spring Boot Actuator for `/actuator/health`

**Frontend**
- Next.js 16 (App Router) with Server Actions
- React 19, TypeScript, Tailwind CSS 4
- `next/font` self-hosting Crimson Pro / Inter / JetBrains Mono
- `react-markdown` for assistant replies
- Editorial Nordic design — single light theme

**Infrastructure**
- Railway monorepo deployment, two services
- Backend reachable only via internal DNS (`backend.railway.internal`)
- Frontend Server Actions bridge browser to backend — API key never leaves
  the backend, no CORS configuration needed

**Tests**
- 14 backend tests (JUnit 5 + WireMock + MockMvc)
- 26 frontend tests (Vitest + React Testing Library)

## Architecture

Hexagonal layout — domain layer has no Spring imports:

```
src/main/java/se/devmentor/
├── web/             — REST controller, exception handler, DTOs
├── application/     — ChatService orchestrates the chat flow
├── domain/          — Personality, Message, LlmClient port, ConversationStore port
├── infrastructure/  — OpenRouter adapter, in-memory store
└── config/          — @ConfigurationProperties beans, OpenAPI config
```

The frontend mirrors the idea: Server Actions are the only path from UI to
backend. Browser code never knows the backend URL or sees the API key — all
of that happens server-side inside the Next.js container.

## Local development

### Backend

1. Sign up at [openrouter.ai](https://openrouter.ai) and generate an API key.
   Top up $5 of credits (Claude 3.5 Haiku is the default; free models also
   work via the `OPENROUTER_MODEL` env var).

2. Create `.env`:

   ```bash
   cp .env.example .env
   # edit .env, fill in OPENROUTER_API_KEY
   ```

3. Run:

   ```bash
   JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
   ```

4. Verify:
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health: http://localhost:8080/actuator/health

### Frontend

In a separate terminal:

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Open http://localhost:3000.

### Tests

```bash
mvn test                   # backend (14 tests)
cd frontend && npm test    # frontend (26 tests)
```

## API

`POST /api/v1/chat`

```json
{
  "personality": "senior-architect",
  "message": "Should I use Postgres or MongoDB?",
  "sessionId": "optional-uuid-to-continue-conversation"
}
```

Returns `{ sessionId, personality, reply }`.

`DELETE /api/v1/chat/{sessionId}` — clears the in-memory history for a session.

Full schema at `/swagger-ui.html`.

## Known limitations

- **In-memory backend store** — conversations vanish on backend restart. The
  frontend's localStorage keeps session IDs visible in History, but the
  server side has nothing to rehydrate from.
- **No authentication** — a session ID is an opaque UUID. Anyone who guesses
  one can read that conversation. A real product would bind sessions to JWT.
- **Single instance only** — sliding-window history is per-process, so
  horizontal scaling would need external session storage.
- **No streaming** — backend returns the full LLM reply in one response. The
  frontend shows a typing indicator during the await.
