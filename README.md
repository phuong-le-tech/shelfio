# Inventory Management Application

A full-stack inventory management application that lets users organize items into custom lists, track their status, and manage inventory through a modern dashboard. Built with a Spring Boot REST API and a React single-page application, containerized with Docker for easy deployment.

## Features

- **Authentication** — Email/password login with JWT tokens stored in HTTP-only cookies, optional Google OAuth2, email verification, password reset
- **Dashboard** — Overview with inventory statistics (total items, status breakdown, category counts)
- **Item Lists** — Create and manage custom lists to organize inventory, with custom field definitions per list
- **Items** — Add, edit, and delete items with image upload, status tracking, and search/filtering
- **Admin Panel** — User management for admin-role users
- **Responsive UI** — Sidebar navigation with a modern, animated interface

## Tech Stack

| Layer           | Technologies                                                              |
| --------------- | ------------------------------------------------------------------------- |
| **Backend**     | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, Lombok |
| **Auth**        | JWT (jjwt), Google OAuth2 (optional)                                      |
| **Database**    | PostgreSQL 16 (production/dev), H2 in-memory (standalone dev)             |
| **Frontend**    | React 18, TypeScript, Vite 7, TailwindCSS, Radix UI, React Hook Form, Zod |
| **Animations**  | Motion (Framer Motion)                                                    |
| **HTTP Client** | Axios                                                                     |
| **Containers**  | Docker, Docker Compose                                                    |
| **API Testing** | Bruno (collection in `inventory_api_request/`)                            |

## Project Structure

```
inventory_app/
├── src/
│   ├── backend/          # Spring Boot API (Maven)
│   └── frontend/         # React SPA (Vite)
├── docker-compose.dev.yml   # Dev: backend + frontend + PostgreSQL + Adminer
├── docker-compose.yml       # Prod: backend + frontend (external DB)
├── .env.dev                 # Dev environment variables
└── inventory_api_request/   # Bruno API request collection
```

## Prerequisites

- **Docker** and **Docker Compose** (recommended)
- Or for local development without Docker:
  - Java 21
  - Node.js 20+
  - PostgreSQL 16 _(optional — H2 is used when running the backend standalone)_

---

## Getting Started Locally

### Option 1: Docker Compose (recommended)

Start all services (backend, frontend, PostgreSQL, Adminer) with a single command:

```bash
docker compose -f docker-compose.dev.yml up --build
```

Once running:

| Service         | URL                       |
| --------------- | ------------------------- |
| Frontend        | http://localhost:3000     |
| Backend API     | http://localhost:8080/api |
| Adminer (DB UI) | http://localhost:8081     |

**Adminer credentials:**

| Field    | Value      |
| -------- | ---------- |
| System   | PostgreSQL |
| Server   | postgres   |
| Username | postgres   |
| Password | postgres   |
| Database | inventory  |

To stop everything:

```bash
docker compose -f docker-compose.dev.yml down
```

---

### Option 2: Run Backend & Frontend Separately

#### 1. Start the Backend

The backend can run standalone using an embedded H2 in-memory database (no PostgreSQL needed):

```bash
cd src/backend
./mvnw spring-boot:run
```

The API starts on **http://localhost:8080**. An H2 console is available at http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:inventorydb`).

> **Tip:** To run against a local PostgreSQL instead of H2, set the `DB_*` environment variables defined in `.env.dev` and ensure your PostgreSQL instance is running.

#### 2. Start the Frontend

```bash
cd src/frontend
npm install
npm run dev
```

The dev server starts on **http://localhost:5173** and automatically proxies `/api` requests to the backend at `http://localhost:8080`.

---

## Environment Variables

Copy `.env.example` to `.env.dev` and fill in the values. Key variables:

| Variable                 | Required | Default                         | Description                                              |
| ------------------------ | -------- | ------------------------------- | -------------------------------------------------------- |
| `DB_HOST`                | Yes      | `localhost`                     | PostgreSQL host                                          |
| `DB_PORT`                | No       | `5432`                          | PostgreSQL port                                          |
| `DB_NAME`                | Yes      | `inventory`                     | Database name                                            |
| `DB_USERNAME`            | Yes      | `postgres`                      | Database user                                            |
| `DB_PASSWORD`            | Yes      | `postgres`                      | Database password                                        |
| `SPRING_PROFILES_ACTIVE` | No       | `dev`                           | Spring profile (`dev`, `prod`)                           |
| `CORS_ALLOWED_ORIGINS`   | No       | `http://localhost:5173,http://localhost:3000` | CORS allowed origins (comma-separated)    |
| `JWT_SECRET`             | Yes      | _(dev key)_                     | Secret for signing JWT tokens (min 32 chars)             |
| `JWT_EXPIRATION_MS`      | No       | `86400000` (24 h)               | JWT token lifetime in milliseconds                       |
| `COOKIE_SECURE`          | No       | `false`                         | Set `true` in production (HTTPS)                         |
| `FRONTEND_URL`           | No       | `http://localhost:5173`         | Frontend URL for email links                             |
| `GOOGLE_CLIENT_ID`       | No       | —                               | Google OAuth2 client ID (optional)                       |
| `EMAIL_PROVIDER`         | No       | `noop`                          | Email provider (`noop` = console, `resend` = Resend API) |
| `EMAIL_FROM`             | No       | `noreply@example.com`           | Sender email address                                     |
| `RESEND_API_KEY`         | No       | —                               | Resend API key (required when `EMAIL_PROVIDER=resend`)   |
| `SENTRY_DSN`             | No       | —                               | Backend Sentry DSN (optional, leave empty to disable)    |
| `VITE_SENTRY_DSN`        | No       | —                               | Frontend Sentry DSN (optional, bundled into JS)          |
