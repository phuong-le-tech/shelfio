# External Services & Monitoring

All third-party services used by Shelfio, with links to their dashboards for monitoring usage, costs, and quotas.

## Email — Resend

Sends verification emails, password reset emails.

| | |
|---|---|
| **Dashboard** | https://resend.com/overview |
| **Usage & Limits** | https://resend.com/settings/billing |
| **API Keys** | https://resend.com/api-keys |
| **Domain Verification** | https://resend.com/domains |
| **Logs (sent/bounced/failed)** | https://resend.com/emails |
| **Free Tier** | 100 emails/day, 3,000/month |
| **Env Vars** | `RESEND_API_KEY`, `EMAIL_FROM` |

## AI Image Analysis — Google Gemini

Powers the "Analyze with AI" feature for item image recognition.

| | |
|---|---|
| **Dashboard** | https://aistudio.google.com |
| **API Keys** | https://aistudio.google.com/apikey |
| **Usage & Quotas** | https://console.cloud.google.com/apis/api/generativelanguage.googleapis.com/quotas |
| **Billing** | https://console.cloud.google.com/billing |
| **Free Tier** | 15 RPM (requests per minute) on Flash models with billing enabled |
| **Env Vars** | `GEMINI_API_KEY`, `GEMINI_MODEL` |

> **Note:** `gemini-2.0-flash` is deprecated for new API keys. Use `gemini-2.5-flash` or newer.

## Payments — Stripe

Handles premium upgrade checkout (one-time €2 payment).

| | |
|---|---|
| **Dashboard** | https://dashboard.stripe.com |
| **Payments** | https://dashboard.stripe.com/payments |
| **Webhooks** | https://dashboard.stripe.com/webhooks |
| **API Keys** | https://dashboard.stripe.com/apikeys |
| **Test Mode** | https://dashboard.stripe.com/test/payments |
| **Env Vars** | `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` |

## Authentication — Google OAuth

Google sign-in/sign-up via OAuth2.

| | |
|---|---|
| **Console** | https://console.cloud.google.com/apis/credentials |
| **OAuth Consent Screen** | https://console.cloud.google.com/apis/credentials/consent |
| **Usage** | https://console.cloud.google.com/apis/api/people.googleapis.com/quotas |
| **Env Vars** | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` |

## Image Storage — Cloudflare R2

Stores item images as objects in R2 buckets.

| | |
|---|---|
| **Dashboard** | https://dash.cloudflare.com → R2 Object Storage |
| **Usage & Billing** | https://dash.cloudflare.com → R2 → Overview |
| **Free Tier** | 10 GB storage, 10 million Class A ops/month, 1 million Class B ops/month |
| **Env Vars** | `R2_ENDPOINT`, `R2_ACCESS_KEY`, `R2_SECRET_KEY`, `R2_BUCKET` |

## Error Monitoring — Sentry

Captures backend exceptions and frontend errors.

| | |
|---|---|
| **Dashboard** | https://sentry.io (project-specific URL depends on your org) |
| **Issues** | https://sentry.io/issues/ |
| **Performance** | https://sentry.io/performance/ |
| **Usage & Billing** | https://sentry.io/settings/billing/ |
| **Free Tier** | 5,000 errors/month, 10,000 transactions/month |
| **Env Vars** | `SENTRY_DSN` (backend), `VITE_SENTRY_DSN` (frontend) |

## Hosting — Railway

Application deployment (backend + frontend + PostgreSQL).

| | |
|---|---|
| **Dashboard** | https://railway.com/dashboard |
| **Usage & Billing** | https://railway.com/account/billing |
| **Logs** | Project → Service → Logs tab |
| **Metrics** | Project → Service → Metrics tab |

## Quick Reference

| Service | What to Watch | Alert If |
|---------|--------------|----------|
| Resend | Daily send count | Approaching 100/day limit |
| Gemini | RPM usage, billing charges | Unexpected cost spike |
| Stripe | Failed payments, webhook failures | Webhook endpoint errors |
| Google OAuth | API quota usage | Quota exceeded errors |
| Cloudflare R2 | Storage size, operations count | Approaching free tier limits |
| Sentry | Error volume | Spike in unhandled exceptions |
| Railway | CPU/memory, deploy status | Service restarts, OOM |
