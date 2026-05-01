# Nordic Dev Mentor — frontend

Next.js (App Router) frontend that consumes the Spring Boot backend in the
parent repo via Server Actions. Deployed alongside backend on Railway as a
second service.

## Local development

1. Copy env template:

   ```bash
   cp .env.example .env.local
   ```

   Edit if your local backend runs on a non-default port.

2. Start the backend (from repo root):

   ```bash
   JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn spring-boot:run
   ```

3. Start the frontend (this directory):

   ```bash
   npm install
   npm run dev
   ```

   Open http://localhost:3000.

## Production

Deployed automatically from the `main` branch to a Railway service named
`frontend` in the `nordic-dev-mentor` project. Server Actions reach the
backend via Railway internal DNS — `BACKEND_URL=http://backend.railway.internal:8080`
is set as a service env var in Railway, not in any committed file.
