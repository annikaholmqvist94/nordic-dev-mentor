# Nordic Dev Mentor

Middleware-tjänst i Spring Boot som vidarebefordrar chatt-anrop till en LLM (OpenRouter) med konfigurerbara personligheter och konversationsminne.

Labb 1 — AI-Integrerad Spring Boot Service.

## Personligheter

| Wire-värde | Roll |
|---|---|
| `junior-helper` | Tålmodig nybörjarmentor som förklarar från grunden |
| `senior-architect` | Senior arkitekt fokuserad på trade-offs och systemtänk |
| `code-reviewer` | Kort, direkt code reviewer som inte lindar in saker |
| `rubber-duck` | Ställer bara frågor — ger aldrig svar |

## Komma igång

### 1. Skaffa en API-nyckel

Skapa konto på [openrouter.ai](https://openrouter.ai) och generera en API-nyckel (gratis tier finns).

### 2. Lägg in nyckeln

Kopiera `.env.example` till `.env` och fyll i din nyckel:

```bash
cp .env.example .env
# redigera .env
```

I IntelliJ: `Run → Edit Configurations → Environment variables → Load from file` och välj `.env`.

Eller exportera i shellet innan du startar:

```bash
export OPENROUTER_API_KEY=sk-or-v1-...
```

### 3. Starta

```bash
./mvnw spring-boot:run
```

Eller kör `DevMentorApplication.main()` direkt i IntelliJ.

### 4. Testa

- Swagger UI: http://localhost:8080/swagger-ui.html
- API: `POST http://localhost:8080/api/v1/chat`

Exempel:

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "personality": "senior-architect",
    "message": "Should I use PostgreSQL or MongoDB?"
  }'
```

## Arkitektur

```
web/             ← REST-lager: controller, exception handler, DTOs
application/     ← Affärslogik: ChatService orkestrerar allt
domain/          ← Modeller och interfaces (Personality, Message, LlmClient, Store)
infrastructure/  ← Adapters mot omvärlden (OpenRouterClient, InMemoryStore)
config/          ← Bönor, properties, OpenAPI-config
exception/       ← Egna exceptions
```

Beroenden går alltid från ytterlagren mot domain — `domain` har inga Spring-importer.

## Progress

- [x] **Pass 1 — Fundament:** projekt, paketstruktur, DTO:er, controller, application skeleton
- [x] **Pass 2 — Personligheter:** Personality enum med fyra system prompts
- [x] **Pass 3 — WebClient mot LLM:** `OpenRouterClient.complete(...)` mappar till /chat/completions och plockar ut `choices[0].message.content`
- [x] **Pass 4 — Minne:** in-memory sliding window per session i `InMemoryConversationStore`
- [x] **Pass 5 — Säkerhet & Swagger (VG):** API-nyckel via env, Swagger på /swagger-ui.html
- [ ] **Pass 6 — Resiliens (VG):** retry på 5xx + 429 + timeouts, idempotency-key
- [ ] **Pass 7 — Tester:** integration-test med WireMock som mockar OpenRouter

## Begränsningar (godkända för labben)

- In-memory historik försvinner vid omstart
- Skalar inte över flera instanser — sliding window är per process
- Ingen TTL på sessioner — minnet växer tills processen dör
- SessionId är inte kopplad till autentiserad användare — vem som helst som gissar en UUID kan läsa någons konversation. I produktion skulle vi koppla session till JWT.
