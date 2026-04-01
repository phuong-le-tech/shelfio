# Contributing to Shelfio

This document provides guidance on development setup, running tests, and contributing to the Shelfio inventory management application.

## Prerequisites

### For Local Development

- **Node.js** 20+ (for frontend)
- **Java 21** (for backend)
- **PostgreSQL 16** (recommended, or use Docker)
- **Docker & Docker Compose** (recommended for a complete dev stack)

### External Services (Optional for Development)

For full feature testing, you may need:

- **Google OAuth Client ID** — Get from https://console.cloud.google.com/apis/credentials
- **Stripe Keys** — Get test keys from https://dashboard.stripe.com/test/apikeys
- **Gemini API Key** — Get from https://aistudio.google.com/app/apikey
- **Resend API Key** — Get from https://resend.com/api-keys

For local development without these services, the app uses no-op implementations by default (see `.env.example`).

---

## Development Setup

### Option 1: Docker Compose (Recommended)

Start the entire stack (backend, frontend, PostgreSQL, Adminer) with:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up --build
```

Services start on:
- **Frontend:** http://localhost:5173 (Vite dev server with hot reload)
- **Backend API:** http://localhost:8080
- **Database Admin:** http://localhost:8081 (Adminer: postgres/postgres/inventory)

To stop:

```bash
docker compose -f docker-compose.dev.yml down
```

To stop and clear the database:

```bash
docker compose -f docker-compose.dev.yml down -v
```

### Option 2: Manual Local Development

#### Start PostgreSQL

```bash
# Using Docker
docker run --name inventory-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=inventory \
  -p 5432:5432 \
  postgres:16

# Or use your system PostgreSQL
brew services start postgresql  # macOS
```

#### Start Backend

```bash
cd src/backend

# Copy and configure environment
cp ../../.env.example ../../.env.dev
# Edit .env.dev with your database credentials

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Or build and run the JAR
./mvnw clean package -DskipTests
java -jar target/inventory-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Backend runs on http://localhost:8080

#### Start Frontend

```bash
cd src/frontend

# Install dependencies
npm install

# Start dev server (proxies /api to http://localhost:8080)
npm run dev
```

Frontend runs on http://localhost:5173

---

## Environment Configuration

<!-- AUTO-GENERATED -->

### Environment Variables Reference

Copy `.env.example` to `.env.dev` and fill in values:

```bash
cp .env.example .env.dev
```

| Variable | Purpose | Required | Default | Example |
|----------|---------|----------|---------|---------|
| `DB_HOST` | PostgreSQL hostname | Only for Docker | `localhost` | `postgres` |
| `DB_PORT` | PostgreSQL port | Only for Docker | `5432` | `5432` |
| `DB_NAME` | PostgreSQL database name | Only for Docker | `inventory` | `inventory` |
| `DB_USERNAME` | PostgreSQL user | Only for Docker | `postgres` | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | Only for Docker | `postgres` | `postgres` |
| `SPRING_PROFILES_ACTIVE` | Spring profile (dev/prod/test) | Yes | `dev` | `dev` |
| `CORS_ALLOWED_ORIGINS` | Frontend origin(s) | Yes | `http://localhost:5173,http://localhost:3000` | `http://localhost:5173` |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | **Yes** | (unset) | Generate: `openssl rand -base64 32` |
| `JWT_EXPIRATION_MS` | JWT expiration in milliseconds | No | `86400000` (24h) | `86400000` |
| `COOKIE_SECURE` | HTTPS-only cookies | No | `false` (dev) | `false` (dev), `true` (prod) |
| `FRONTEND_URL` | Frontend URL for redirects | Yes | `http://localhost:5173` | `http://localhost:5173` |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | Optional | (unset) | From Google Cloud Console |
| `EMAIL_PROVIDER` | Email backend (noop/resend) | No | `noop` | `noop` (dev), `resend` (prod) |
| `EMAIL_FROM` | Sender email address | If using resend | `noreply@example.com` | `noreply@shelfio.com` |
| `RESEND_API_KEY` | Resend API key | Only if `EMAIL_PROVIDER=resend` | (unset) | From resend.com |
| `STORAGE_PROVIDER` | Image storage (noop/r2) | No | `noop` | `noop` (dev), `r2` (prod) |
| `R2_ENDPOINT` | Cloudflare R2 endpoint | Only if `STORAGE_PROVIDER=r2` | (unset) | `https://xxxxx.r2.cloudflarestorage.com` |
| `R2_ACCESS_KEY` | R2 access key | Only if `STORAGE_PROVIDER=r2` | (unset) | From Cloudflare dashboard |
| `R2_SECRET_KEY` | R2 secret key | Only if `STORAGE_PROVIDER=r2` | (unset) | From Cloudflare dashboard |
| `R2_BUCKET` | R2 bucket name | Only if `STORAGE_PROVIDER=r2` | (unset) | `shelfio-images` |
| `STRIPE_SECRET_KEY` | Stripe API secret key | Only in prod | (unset) | Get from Stripe test/live keys |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook secret | Only in prod | (unset) | Use `stripe listen` in dev |
| `AI_PROVIDER` | AI analysis backend (noop/ollama/gemini) | No | `noop` | `noop` (dev), `gemini` (prod) |
| `OLLAMA_BASE_URL` | Ollama server URL | Only if `AI_PROVIDER=ollama` | `http://localhost:11434` | `http://host.docker.internal:11434` |
| `OLLAMA_MODEL` | Vision model for Ollama | Only if `AI_PROVIDER=ollama` | `gemma3:4b` | `gemma3:4b` |
| `OLLAMA_ALLOWED_HOSTS` | Extra Ollama hosts (comma-separated) | Optional | (unset) | `ollama.railway.internal` |
| `GEMINI_API_KEY` | Google Gemini API key | Only if `AI_PROVIDER=gemini` | (unset) | From aistudio.google.com |
| `GEMINI_MODEL` | Gemini model to use | No | `gemini-2.5-flash` | `gemini-2.5-flash` |
| `GEMINI_TIMEOUT` | Gemini request timeout (seconds) | No | `30` | `30` |
| `GEMINI_THREAD_POOL` | Gemini thread pool size | No | `2` | `2` |
| `SENTRY_DSN` | Sentry error monitoring (backend) | Optional | (unset) | From sentry.io |
| `VITE_SENTRY_DSN` | Sentry error monitoring (frontend) | Optional | (unset) | From sentry.io |

### Generate JWT Secret

```bash
openssl rand -base64 32
```

### Example .env.dev

```bash
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=inventory
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Spring
SPRING_PROFILES_ACTIVE=dev

# Frontend
FRONTEND_URL=http://localhost:5173
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# JWT
JWT_SECRET=CHANGE_ME_TO_A_RANDOM_256_BIT_STRING_FROM_openssl_rand_-base64_32
JWT_EXPIRATION_MS=86400000

# Cookies
COOKIE_SECURE=false

# Email (use no-op in dev)
EMAIL_PROVIDER=noop
EMAIL_FROM=noreply@example.com

# Image Storage (use no-op in dev)
STORAGE_PROVIDER=noop

# AI Analysis (use no-op in dev)
AI_PROVIDER=noop
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=gemma3:4b

# Monitoring (optional)
SENTRY_DSN=
VITE_SENTRY_DSN=

# Stripe (optional in dev)
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=

# Google OAuth (optional in dev)
GOOGLE_CLIENT_ID=
```

<!-- END AUTO-GENERATED -->

---

## Running Tests

### Backend Tests

Run all backend tests:

```bash
cd src/backend
./mvnw test
```

Run a single test class:

```bash
./mvnw test -Dtest=ClassName
```

Run a specific test method:

```bash
./mvnw test -Dtest=ClassName#methodName
```

Run tests with coverage report:

```bash
./mvnw clean test jacoco:report
# View report in target/site/jacoco/index.html
```

### Frontend Tests

Run all frontend tests:

```bash
cd src/frontend
npm test
```

Run tests in watch mode:

```bash
npm run test:watch
```

Run tests with coverage:

```bash
npm run coverage
```

---

## Build Commands

### Backend

```bash
cd src/backend

# Clean build
./mvnw clean package

# Build without tests
./mvnw clean package -DskipTests

# Build Docker image
docker build -f Dockerfile -t shelfio-backend:latest .
```

### Frontend

```bash
cd src/frontend

# Production build
npm run build

# Preview production build locally
npm run preview

# Lint code
npm run lint
```

---

## Database Migrations

Migrations are managed by **Flyway** and applied automatically on startup.

To view applied migrations:

```bash
# Connect to PostgreSQL
psql -U postgres -d inventory -h localhost

# List flyway_schema_history table
SELECT * FROM flyway_schema_history ORDER BY success DESC;
```

### Creating New Migrations

1. Create a new SQL file in `src/backend/src/main/resources/db/migration/`
2. Follow the naming pattern: `V<number>__<description>.sql`
3. Migrations run in version order automatically

Example:

```bash
touch src/backend/src/main/resources/db/migration/V16__add_new_column.sql
```

---

## API Testing

### Using Bruno (Recommended)

The project includes a Bruno request collection in `inventory_api_request/`. Open with [Bruno](https://www.usebruno.com/):

```bash
# Install Bruno CLI (optional)
brew install bruno  # macOS
npm install -g @usebruno/cli

# Run requests
bruno run inventory_api_request/
```

### Using cURL

Example login request:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password123"}'
```

---

## Git Workflow

### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <subject>

<body>
```

Types:
- `feat:` — New feature
- `fix:` — Bug fix
- `refactor:` — Code refactoring (no feature/fix)
- `test:` — Test addition/modification
- `docs:` — Documentation
- `chore:` — Build, CI, dependency updates
- `perf:` — Performance improvement

Example:

```
feat: add workspace activity feed

Implement GET /workspaces/{id}/activity endpoint to track user actions
(item create/update/delete, member management, etc).

Closes #123
```

### Before Creating a PR

1. **Run tests locally:**
   ```bash
   cd src/backend && ./mvnw test
   cd src/frontend && npm test
   ```

2. **Build locally:**
   ```bash
   cd src/backend && ./mvnw clean package
   cd src/frontend && npm run build
   ```

3. **Check code quality:**
   ```bash
   cd src/frontend && npm run lint
   ```

4. **Update relevant documentation** — If your changes affect:
   - API endpoints → Update CLAUDE.md API Endpoints section
   - Database schema → Update CLAUDE.md Database section
   - Environment variables → Update this file's environment table
   - External services → Update docs/EXTERNAL-SERVICES.md
   - Deployment → Update docs/RUNBOOK.md

---

## Development Patterns

### Backend (Spring Boot / Java)

**Service layer validation:**
```java
public void validateListOwnership(UUID listId, UUID userId) {
    ItemList list = itemListRepository.findById(listId)
        .orElseThrow(() -> new ItemListNotFoundException(listId));
    if (!list.getUser().getId().equals(userId)) {
        throw new UnauthorizedException("List ownership validation failed");
    }
}
```

**Entity optimistic locking:**
All entities use `@Version` for concurrent modification detection:
```java
@Entity
public class Item {
    @Version
    private Long version;
}
```

**JWT authentication in tests:**
```java
SecurityContext context = SecurityContextHolder.createEmptyContext();
CustomUserDetails userDetails = new CustomUserDetails(userId, email, Role.USER);
context.setAuthentication(
    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
);
SecurityContextHolder.setContext(context);
```

### Frontend (React / TypeScript)

**API calls with error handling:**
```typescript
const { data, isLoading, error } = useQuery({
    queryKey: ['items', listId],
    queryFn: () => api.getItems(listId),
    enabled: !!listId,
});

if (error) {
    const message = getApiErrorMessage(error);
    // Handle error
}
```

**Form validation with Zod:**
```typescript
import { z } from 'zod';

const itemSchema = z.object({
    name: z.string().min(1, 'Name required'),
    status: z.enum(['AVAILABLE', 'TO_VERIFY']),
    stock: z.number().min(0),
});

type ItemForm = z.infer<typeof itemSchema>;

const form = useForm<ItemForm>({
    resolver: zodResolver(itemSchema),
});
```

---

## Troubleshooting

### Backend won't start

**Check Java version:**
```bash
java -version
# Must be Java 21+
```

**Check PostgreSQL connection:**
```bash
psql -U postgres -h localhost -d inventory
```

**Clear Maven cache and rebuild:**
```bash
cd src/backend
./mvnw clean install
./mvnw spring-boot:run
```

### Frontend dev server won't start

**Clear node_modules and reinstall:**
```bash
cd src/frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

**Check port 5173 is free:**
```bash
lsof -i :5173
```

### Tests fail mysteriously

**Backend — Clear test database:**
```bash
cd src/backend
./mvnw clean test
```

**Frontend — Clear Jest cache:**
```bash
cd src/frontend
npm test -- --clearCache
```

### Docker issues

**Rebuild from scratch:**
```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up --build
```

**Check logs:**
```bash
docker compose -f docker-compose.dev.yml logs -f backend
docker compose -f docker-compose.dev.yml logs -f frontend
```

---

## Common Development Tasks

### Add a New API Endpoint

1. Create controller method in `src/backend/src/main/java/.../controller/`
2. Add service method in `src/backend/src/main/java/.../service/`
3. Write integration tests in `src/backend/src/test/java/.../controller/`
4. Add TypeScript types in `src/frontend/src/types/`
5. Create API service function in `src/frontend/src/services/`
6. Update CLAUDE.md API Endpoints section

### Add a New Database Table

1. Create a Flyway migration file: `V<number>__<description>.sql`
2. Create a JPA entity in `src/backend/src/main/java/.../model/`
3. Create a repository interface extending `JpaRepository`
4. Add tests in `src/backend/src/test/java/.../`
5. Update CLAUDE.md Database section

### Add a New Environment Variable

1. Add to `.env.example` with documentation
2. Add to `src/backend/src/main/resources/application.properties`
3. Add to `docker-compose.dev.yml` (if applicable)
4. Update this CONTRIBUTING.md file

---

## Need Help?

- **Code questions:** Check CLAUDE.md for architecture and patterns
- **API questions:** See CLAUDE.md API Endpoints section or use Bruno request collection
- **External services:** Check docs/EXTERNAL-SERVICES.md for dashboards and keys
- **Deployment issues:** See docs/RUNBOOK.md
