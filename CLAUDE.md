# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Shelfio — Inventory management application with user authentication and role-based access control. Users can organize items into lists, with each item having: Name, Status, Stock, Image, Barcode. Lists support custom field definitions (TEXT, NUMBER, DATE, BOOLEAN) that items can populate. Items can be looked up by scanning barcodes or QR codes via the device camera. Supports USER, PREMIUM_USER, and ADMIN roles. Free users are limited to 5 lists; premium upgrade via one-time €2 Stripe Checkout payment. Includes email verification, password reset, and account self-deletion.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.5, Spring Security (stateless JWT), PostgreSQL, Flyway, Lombok
- **Frontend**: React 18, TypeScript, Vite 7, TailwindCSS, Radix UI, React Hook Form, Zod, Axios, Motion (Framer Motion)
- **Testing**: JUnit 5 + Mockito + AssertJ (backend), Vitest (frontend)

## Build and Run Commands

### Backend (Spring Boot)

```bash
cd src/backend
./mvnw spring-boot:run           # Run the application (uses PostgreSQL)
./mvnw test                       # Run all tests (468 tests)
./mvnw test -Dtest=ClassName      # Run a single test class
./mvnw clean package              # Build JAR
```

### Frontend (React + Vite)

```bash
cd src/frontend
npm install                       # Install dependencies
npm run dev                       # Start dev server (port 5173, proxies /api to backend)
npm run build                     # Production build
npm test                          # Run all tests (32 tests, Vitest)
npm run test:watch                # Watch mode
```

### Docker

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up --build  # Dev: backend + frontend + PostgreSQL + Adminer
docker compose up --build                            # Prod: backend + frontend (external DB)
docker compose down                                  # Stop all services
docker compose down -v                               # Stop and remove volumes (clears database)
```

## Architecture

### Backend Structure

- `controller/` - REST API endpoints (ItemController, ItemListController, AuthController, AdminController, AccountController, StripeController)
- `service/` - Business logic interfaces (IItemService, IItemListService, IAuthService, IUserService, IStripeService, IImageAnalysisService) and implementations, CustomFieldValidator, EmailSender interface, NoOpEmailSender, ResendEmailSender, TokenCleanupService, ImageProcessingService, OllamaImageAnalysisService, NoOpImageAnalysisService
- `repository/` - Data access (JpaRepository), specification/ItemSpecification and ItemListSpecification and UserSpecification for dynamic filtering, VerificationTokenRepository, StripeWebhookEventRepository
- `model/` - JPA entities (Item, ItemList, User, VerificationToken, StripeWebhookEvent) with `@Version` for optimistic locking
- `dto/` - Request/Response objects for API: LoginRequest, SignupRequest, GoogleAuthRequest, ForgotPasswordRequest, ResetPasswordRequest, ResendVerificationRequest, CreateUserRequest, UpdateStatusRequest, ItemRequest (includes barcode field), ItemListRequest, ItemSearchCriteria; Responses: AuthResponse, UserResponse, ItemResponse (includes barcode field), ItemListResponse, DashboardStats, AdminDashboardStats, AdminItemResponse, AdminItemListResponse, AdminUserDetailResponse, ApiDataResponse, PageResponse, ApiErrorResponse, ImageAnalysisResult; CustomFieldDefinition
- `config/` - SecurityConfig, DataInitializer, ApiResponseWrappingAdvice, RestTemplateConfig, RateLimitConfig, StripeConfig
- `security/` - JWT authentication (JwtService, JwtAuthenticationFilter, CustomUserDetails, CustomUserDetailsService, CookieService, SecurityUtils, LoginRateLimiter), API rate limiting (ApiRateLimiter, ApiRateLimitFilter)
- `enums/` - Role (USER, PREMIUM_USER, ADMIN), ItemStatus (AVAILABLE, TO_VERIFY, NEEDS_MAINTENANCE, DAMAGED), CustomFieldType (TEXT, NUMBER, DATE, BOOLEAN), TokenType (EMAIL_VERIFICATION, PASSWORD_RESET)
- `exception/` - Custom exceptions (ItemNotFoundException, ItemListNotFoundException, UserNotFoundException, UserAlreadyExistsException, UnauthorizedException, FileValidationException, CustomFieldValidationException, RateLimitExceededException, AccountNotVerifiedException, ListLimitExceededException, ServiceUnavailableException)
- `util/` - ImageContentValidator (magic byte detection + dimension validation for uploaded images)

### Frontend Structure

- `pages/` - Dashboard, ListsPage, ListDetail, ListForm, ItemForm, LoginPage, SignupPage, ForgotPassword, ResetPassword, VerifyEmail, NotFound, PrivacyPolicy, TermsOfService, UpgradePage, PaymentSuccess, PaymentCancel, admin/UsersPage, admin/UserDetailPage, admin/AdminDashboardPage, admin/AdminListsPage, admin/AdminListDetailPage
- `components/` - Layout, UserMenu, ProtectedRoute, Skeleton, Toast, ErrorBoundary, ConfirmModal, ListCombobox, GoogleAuthButton, RoleBadge, BarcodeScanner, BarcodeScannerModal, ImageAnalysisSuggestions
  - `ui/` - Radix-based primitives (avatar, badge, button, dialog, dropdown-menu, input, label, separator, skeleton, table, textarea, tooltip)
  - `effects/` - Animation components (blur-fade, dot-pattern, spotlight-card, staggered-list)
- `hooks/` - useDashboardStats (dashboard data fetching), useDebounce, useImageAnalysis (AI image analysis lifecycle)
- `contexts/` - AuthContext for authentication state
- `services/` - api.ts (items), authApi.ts (authentication), stripeApi.ts (Stripe checkout/status), http.ts (Axios instance setup)
- `types/` - item.ts (includes formatCustomFieldValue, formatStatus utilities), auth.ts
- `utils/` - errorUtils.ts (getApiErrorMessage, getApiErrorStatus), avatarUtils.ts
- `lib/` - utils.ts (cn helper for tailwind-merge + clsx)
- `schemas/` - auth.schemas.ts, item.schemas.ts (Zod validation)
- `__tests__/` - Vitest unit tests for utilities

### API Endpoints

#### Authentication (`/api/v1/auth`)

- `POST /api/v1/auth/login` - User login (returns JWT in cookie, rate-limited: 5 attempts/60s per IP)
- `POST /api/v1/auth/signup` - User self-registration (email/password, USER role, sends verification email, rate-limited)
- `POST /api/v1/auth/google` - Google OAuth sign-in/sign-up (receives access token, verifies via Google userinfo API, creates/links user)
- `GET /api/v1/auth/verify?token=` - Verify email address via token
- `POST /api/v1/auth/resend-verification` - Resend verification email
- `POST /api/v1/auth/forgot-password` - Request password reset email
- `POST /api/v1/auth/reset-password` - Reset password via token
- `POST /api/v1/auth/logout` - User logout
- `GET /api/v1/auth/me` - Get current user

#### Account (`/api/v1/account`)

- `DELETE /api/v1/account` - Delete own account (authenticated)

#### Admin (`/api/v1/admin`) - Requires ADMIN role

- `GET /api/v1/admin/users` - List all users (paginated, searchable)
- `GET /api/v1/admin/users/{id}` - Get user detail
- `POST /api/v1/admin/users` - Create new user
- `DELETE /api/v1/admin/users/{id}` - Delete user
- `PATCH /api/v1/admin/users/{id}/role` - Update user role
- `PATCH /api/v1/admin/users/{id}/status` - Update user enabled status
- `POST /api/v1/admin/users/{id}/reset-password` - Admin-initiated password reset
- `GET /api/v1/admin/stats` - Admin dashboard statistics
- `GET /api/v1/admin/lists` - List all item lists (paginated, filterable by user/category/search)
- `GET /api/v1/admin/lists/{id}` - Get list detail
- `GET /api/v1/admin/items` - List all items (paginated, filterable)

#### Item Lists (`/api/v1/lists`)

- `GET /api/v1/lists` - List all item lists (paginated, user-scoped)
- `GET /api/v1/lists/{id}` - Get list metadata (no items eager-loaded)
- `POST /api/v1/lists` - Create list
- `PATCH /api/v1/lists/{id}` - Update list
- `DELETE /api/v1/lists/{id}` - Delete list

#### Items (`/api/v1/items`)

- `GET /api/v1/items` - List all items (paginated, filterable)
- `GET /api/v1/items/{id}` - Get single item
- `GET /api/v1/items/{id}/image` - Get item image
- `POST /api/v1/items` - Create item (multipart: data + image, validates list ownership)
- `PATCH /api/v1/items/{id}` - Update item (multipart: data + image, validates list ownership)
- `DELETE /api/v1/items/{id}` - Delete item
- `GET /api/v1/items/barcode/{code}` - Look up item by barcode (user-scoped, returns 404 if not found)
- `GET /api/v1/items/stats` - Dashboard statistics (user-scoped, admin sees global)
- `POST /api/v1/items/analyze-image` - Submit image for AI analysis (multipart: image + optional listId, returns 202 with analysisId, 503 if AI disabled)
- `GET /api/v1/items/analyze-image/{analysisId}` - Poll analysis result (PENDING/COMPLETED/FAILED, user-scoped)

#### Stripe (`/api/v1/stripe`)

- `POST /api/v1/stripe/checkout` (authenticated) - Create Stripe Checkout Session, returns `{ url }` for redirect. Rejects ADMIN (403) and existing PREMIUM_USER (400)
- `POST /api/v1/stripe/webhook` (public, raw body) - Handle Stripe webhook events. Verifies signature, processes `checkout.session.completed` to upgrade user to PREMIUM_USER
- `GET /api/v1/stripe/status` (authenticated) - Check premium status, returns `{ premium: true/false }`

### Database

#### Tables

- `users` - id (UUID), email, password, role (USER/PREMIUM_USER/ADMIN), google_id, picture_url, stripe_customer_id, stripe_payment_id, enabled, created_at, updated_at, version
- `item_lists` - id (UUID), name, description, category, custom_field_definitions (JSON), user_id (FK), created_at, updated_at, version
- `items` - id (UUID), name, status (AVAILABLE/TO_VERIFY/NEEDS_MAINTENANCE/DAMAGED), stock (INTEGER), barcode (VARCHAR), image_data (BYTEA), content_type, custom_field_values (JSON), item_list_id (FK), created_at, updated_at, version
- `verification_tokens` - id (UUID), token, user_id (FK), type (EMAIL_VERIFICATION/PASSWORD_RESET), expires_at, created_at
- `stripe_webhook_events` - id (UUID), stripe_event_id (UNIQUE), event_type, processed_at — idempotency tracking for Stripe webhooks

#### Relationships

- User -> ItemList (one-to-many, cascade delete)
- ItemList -> Item (one-to-many, cascade delete)
- User -> VerificationToken (one-to-many, cascade delete)

#### Migrations

- Flyway managed (enabled in prod profile, disabled in dev where Hibernate auto-updates)
- `V1__init_schema.sql` — baseline (users, item_lists, items, verification_tokens)
- `V2__add_premium_fields.sql` — adds stripe_customer_id, stripe_payment_id to users
- `V3__add_stripe_webhook_events.sql` — creates stripe_webhook_events table for webhook idempotency
- `V4__add_stripe_customer_id_index.sql` — adds index on stripe_customer_id
- `V5__add_stripe_payment_id_index.sql` — adds index on stripe_payment_id
- `V6__rename_item_status_values.sql` — migrates item status from stock-based (IN_STOCK/LOW_STOCK/OUT_OF_STOCK) to condition-based (AVAILABLE/TO_VERIFY/NEEDS_MAINTENANCE/DAMAGED)
- `V11__add_barcode_column.sql` — adds barcode (VARCHAR) column and index to items table

### Security Architecture

- **Authentication**: Stateless JWT stored in HttpOnly cookie (`access_token`) with `SameSite=Lax`
- **JWT filter**: Builds `CustomUserDetails` from JWT claims (userId, email, role) — no DB query per request
- **CSRF**: Disabled — `SameSite=Lax` cookie attribute prevents cross-origin state-changing requests
- **Authorization**: Role-based (USER, PREMIUM_USER, ADMIN) via Spring Security `@EnableMethodSecurity`
- **Ownership enforcement**: Service layer checks list ownership on item create/update (IDOR prevention via `findListWithOwnershipCheck`)
- **Password**: BCrypt hashing, signup requires 12+ chars with uppercase, lowercase, and digit
- **Auth rate limiting**: Login, signup, Google auth, verify, resend-verification, forgot-password, reset-password endpoints limited to 5 attempts per IP per 60-second sliding window (LoginRateLimiter)
- **API rate limiting**: Global rate limit of 100 requests/minute per user (authenticated) or IP (anonymous) via servlet filter (ApiRateLimitFilter); excludes `/api/v1/auth/` and `/actuator/` paths
- **Email verification**: Signup sends verification email; unverified accounts blocked from login
- **Google OAuth**: GIS token flow — frontend gets access token via popup, backend verifies via Google userinfo API
- **Image validation**: Magic byte detection (JPEG, PNG, GIF, WebP) independent of MIME type
- **CORS**: Configured per-environment via `app.cors.allowed-origins`
- **Cookie config**: `app.cookie.secure` (true in prod, false in dev), `app.cookie.domain`
- **File upload limits**: 10MB max via `spring.servlet.multipart.max-file-size`
- **Security headers**: X-Frame-Options (DENY), CSP (with frame-ancestors 'none'), X-Content-Type-Options (nosniff), Referrer-Policy (strict-origin-when-cross-origin), Permissions-Policy (blocks camera/mic/geo)
- **JWT dev secret guard**: `JwtService` fails fast if dev-only secret is used with `prod` profile
- **Stripe webhook security**: Signature verification via `Webhook.constructEvent`; webhook endpoint is public (no auth cookie); idempotency via `stripe_webhook_events` table deduplication
- **List limit enforcement**: Free users (USER role) limited to 5 lists; enforced with pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`) to prevent race conditions on concurrent creation
- **Stripe config fail-fast**: `StripeConfig` throws `IllegalStateException` in prod profile if secret key or webhook secret is missing

### Testing Architecture

- **Backend (468 tests)**: JUnit 5, Mockito, AssertJ, MockMvc, H2 in-memory DB for test profile
  - Unit tests: ItemServiceImpl, ItemListServiceImpl, AuthServiceImpl, StripeServiceImpl, CustomFieldValidator, JwtService
  - Integration tests: ItemController (with real DB), AuthorizationBoundary (with real security filters)
  - Exception handler tests: GlobalExceptionHandler (all exception types)
  - Test fixtures: `factory/fixture/` (UserFixture, ItemFixture, ItemListFixture) for test data builders
  - Test config: `TestSecurityConfig` disables filters (imported only for unit-style controller tests)
  - SecurityContext setup: `CustomUserDetails(userId, email, role)` constructor for test auth
- **Frontend (32 tests)**: Vitest with globals
  - Utility tests: formatCustomFieldValue, formatStatus, slugify, errorUtils
  - Schema tests: signupSchema validation
  - Auth tests: role-based `isPremium` logic for USER, PREMIUM_USER, ADMIN

### Key Patterns

- **Bean validation bypass fix**: ItemController manually validates via injected `jakarta.validation.Validator` after `objectMapper.readValue()` (multipart form data skips `@Valid`)
- **User-scoped data**: Dashboard stats and list queries filter by authenticated user; admins see global data
- **DRY auth helper**: `requireCurrentUserId()` in ItemListServiceImpl eliminates duplicate admin-check patterns
- **Optimistic locking**: All entities use `@Version` for concurrent modification detection
- **Timestamps**: Item entity uses `@CreationTimestamp`/`@UpdateTimestamp`; User entity uses manual `@PrePersist`/`@PreUpdate`
- **Email abstraction**: `EmailSender` interface with `NoOpEmailSender` (dev/test) and `ResendEmailSender` (production) implementations, selected via `app.email.provider` config
- **Token cleanup**: `TokenCleanupService` scheduled task removes expired verification tokens
- **Pessimistic locking for list limits**: `userRepository.findByIdWithLock()` uses `@Lock(PESSIMISTIC_WRITE)` to serialize concurrent list creation and prevent exceeding the 5-list free tier limit
- **Webhook idempotency**: `stripe_webhook_events` table with unique `stripe_event_id` prevents duplicate processing of Stripe events; checked before processing, recorded after
- **Stripe Checkout (server-side redirect)**: Backend creates Checkout Session with inline price data (no pre-created Stripe Product/Price needed), frontend redirects to Stripe URL — no `@stripe/stripe-js` dependency
- **Frontend-only barcode scanning**: Uses html5-qrcode library for barcode/QR code scanning via device camera; no backend barcode processing — scanning and decoding happen entirely in the browser
- **AI image analysis**: Strategy pattern with `IImageAnalysisService` — `OllamaImageAnalysisService` (active when `AI_PROVIDER=ollama`), `GeminiImageAnalysisService` (active when `AI_PROVIDER=gemini`), and `NoOpImageAnalysisService` (default). Async analysis via thread pool, results stored in-memory with 10-min TTL. Frontend polls every 2s with 90s timeout. Ollama base URL validated against configurable allowed hosts (SSRF prevention). `OLLAMA_ALLOWED_HOSTS` env var adds extra hosts beyond default `localhost`/`127.0.0.1`/`host.docker.internal`

### Environment Configuration

- `application.properties` — shared defaults (PostgreSQL, CORS, JWT, cookie, email, Stripe, Sentry settings)
- `application-dev.properties` — dev overrides (ddl-auto=update, Flyway disabled, show-sql=true)
- `application-prod.properties` — prod overrides (ddl-auto=validate, Flyway enabled with baseline, show-sql=false)
- `application-test.properties` — test profile (H2 in-memory, bean overriding enabled)
- `RestTemplate` — configured with 5s connect / 10s read timeout for external API calls (Google userinfo)
- `logback-spring.xml` — structured JSON logging via logstash-logback-encoder
- Stripe config: `stripe.secret-key`, `stripe.webhook-secret`, `stripe.price-amount` (default 200 = €2.00), `stripe.price-currency` (default eur)
- AI config: `app.ai.provider` (noop/ollama/gemini), `app.ai.ollama.base-url`, `app.ai.ollama.model`, `app.ai.ollama.timeout-seconds`, `app.ai.ollama.thread-pool-size`, `app.ai.ollama.allowed-hosts` (comma-separated extra hosts for SSRF whitelist), `app.ai.gemini.api-key`, `app.ai.gemini.model`, `app.ai.gemini.timeout-seconds`, `app.ai.gemini.thread-pool-size`
