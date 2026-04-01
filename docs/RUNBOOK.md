# Shelfio Operations Runbook

This document covers production deployment, health checks, monitoring, and incident response for Shelfio.

---

## Deployment

### Prerequisites

- Docker & Docker Compose (or Kubernetes)
- PostgreSQL 16+
- All external service keys configured (Stripe, Gemini, Resend, etc.)
- Domain name with HTTPS certificate

### Environment Setup

Create a `.env.prod` file with production values:

```bash
# Database (external PostgreSQL)
DB_HOST=your-db-host.example.com
DB_PORT=5432
DB_NAME=inventory
DB_USERNAME=postgres
DB_PASSWORD=<strong-password>

# Spring
SPRING_PROFILES_ACTIVE=prod

# Frontend
FRONTEND_URL=https://shelfio.example.com
CORS_ALLOWED_ORIGINS=https://shelfio.example.com

# JWT (generate new with: openssl rand -base64 32)
JWT_SECRET=<long-random-string-min-32-chars>
JWT_EXPIRATION_MS=86400000

# Cookies
COOKIE_SECURE=true

# Email (required in production)
EMAIL_PROVIDER=resend
EMAIL_FROM=noreply@shelfio.example.com
RESEND_API_KEY=<your-resend-api-key>

# Image Storage (required in production)
STORAGE_PROVIDER=r2
R2_ENDPOINT=https://xxxxx.r2.cloudflarestorage.com
R2_ACCESS_KEY=<your-r2-access-key>
R2_SECRET_KEY=<your-r2-secret-key>
R2_BUCKET=shelfio-images
R2_REGION=auto
R2_PRESIGNED_URL_EXPIRY_MINUTES=15

# AI Analysis (production-ready with Gemini)
AI_PROVIDER=gemini
GEMINI_API_KEY=<your-gemini-api-key>
GEMINI_MODEL=gemini-2.5-flash
GEMINI_TIMEOUT=30
GEMINI_THREAD_POOL=4

# Stripe (required for premium features)
STRIPE_SECRET_KEY=sk_live_xxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxx

# Google OAuth (required for sign-in)
GOOGLE_CLIENT_ID=<your-google-client-id>

# Monitoring (recommended)
SENTRY_DSN=<your-backend-sentry-dsn>
VITE_SENTRY_DSN=<your-frontend-sentry-dsn>

# Admin seed (set once on first deploy, then remove)
ADMIN_EMAIL=admin@shelfio.example.com
ADMIN_PASSWORD=<temp-strong-password>
```

### Docker Compose Deployment

**1. Prepare production Docker Compose file:**

```bash
cp docker-compose.yml docker-compose.prod.yml
```

**2. Start the application:**

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

**3. Check status:**

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend frontend
```

**4. Run database migrations:**

Migrations apply automatically on startup. Monitor the logs:

```bash
docker compose -f docker-compose.prod.yml logs backend | grep -i "flyway\|migration"
```

### Kubernetes Deployment (Advanced)

If deploying to Kubernetes:

1. Create secrets from `.env.prod`:
   ```bash
   kubectl create secret generic shelfio-env --from-env-file=.env.prod
   ```

2. Use environment variables in deployment manifests
3. Configure:
   - Resource requests/limits
   - Health check probes
   - Persistent volume for database (if needed)
   - Network policies for external services

---

## Health Checks

### Liveness Probes

Backend health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Expected response (HTTP 200):

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

### Readiness Probes

```bash
curl http://localhost:8080/actuator/health/readiness
```

Expected response (HTTP 200):

```json
{
  "status": "UP"
}
```

### Database Connectivity

```bash
curl http://localhost:8080/actuator/health/db
```

Expected response:

```json
{
  "status": "UP",
  "details": {
    "database": "PostgreSQL",
    "validationQuery": "isValid()"
  }
}
```

### Frontend Health

```bash
curl -I http://localhost:3000/
```

Expected response: `HTTP 200` with HTML content

---

## Monitoring & Logging

### Access Logs

Backend (stdout in Docker):

```bash
docker compose -f docker-compose.prod.yml logs backend
```

Frontend (stdout in Docker):

```bash
docker compose -f docker-compose.prod.yml logs frontend
```

### Structured JSON Logging

The backend emits structured JSON logs via Logstash Logback Encoder:

```json
{
  "@timestamp": "2026-04-01T10:30:00Z",
  "level": "INFO",
  "logger_name": "com.inventory.service.ItemServiceImpl",
  "message": "Item created successfully",
  "thread_name": "http-nio-8080-exec-1",
  "trace_id": "abc123def456",
  "user_id": "550e8400-e29b-41d4-a716-446655440000",
  "item_id": "660e8400-e29b-41d4-a716-446655440000",
  "duration_ms": 150
}
```

Integrate with log aggregation (ELK, Datadog, Sentry) by:

1. Forwarding Docker logs to a log aggregator
2. Parsing JSON format
3. Filtering on `"level": "ERROR"` for alerts

### Sentry Integration

When `SENTRY_DSN` is configured, exceptions are sent to [sentry.io](https://sentry.io):

**Backend errors** (automatic):
- Unhandled exceptions
- HTTP 5xx responses
- Authentication failures
- Rate limit violations

**Frontend errors** (automatic):
- JavaScript exceptions
- React error boundaries
- HTTP error responses
- Network timeouts

**Manual tracking** (optional):

```typescript
// Frontend
import * as Sentry from "@sentry/react";

Sentry.captureException(error);
```

```java
// Backend
Sentry.captureException(exception);
```

---

## Key Metrics to Monitor

| Metric | Threshold | Action |
|--------|-----------|--------|
| **API Response Time** | > 1000ms | Check database performance, consider caching |
| **CPU Usage** | > 80% | Scale horizontally or vertically |
| **Memory Usage** | > 85% | Increase JVM heap size or restart services |
| **Database Connections** | > 80 of max | Increase pool size or check for connection leaks |
| **Error Rate** | > 1% of requests | Review recent errors in Sentry |
| **Email Send Failures** | > 5% | Check Resend API quota, domain verification |
| **Image Upload Failures** | > 2% | Check R2 bucket permissions, quota |
| **AI Analysis Failures** | > 10% | Check Gemini API quota, rate limits |
| **Stripe Webhook Failures** | > 0 | Verify webhook secret, endpoint accessibility |
| **Disk Space (DB)** | < 10% free | Plan for storage expansion |
| **Disk Space (Container)** | < 5% free | Clean up logs, Docker images |

---

## Common Issues & Solutions

### 1. Database Connection Pool Exhausted

**Symptoms:** Requests hang, "Unable to acquire a connection" errors

**Diagnosis:**
```bash
# Check active connections
psql -U postgres -d inventory -c "SELECT count(*) FROM pg_stat_activity WHERE datname = 'inventory';"

# Check connection pool config
docker compose -f docker-compose.prod.yml exec backend env | grep -i datasource
```

**Solutions:**
- Increase pool size in `application-prod.properties`: `spring.datasource.hikari.maximum-pool-size=20`
- Kill idle connections in PostgreSQL:
  ```sql
  SELECT pg_terminate_backend(pg_stat_activity.pid)
  FROM pg_stat_activity
  WHERE pg_stat_activity.datname = 'inventory'
  AND pg_stat_activity.pid <> pg_backend_pid()
  AND state = 'idle';
  ```
- Restart the service: `docker compose restart backend`

### 2. Out of Memory (OOM)

**Symptoms:** Unexpected service restarts, "Cannot allocate memory" logs

**Diagnosis:**
```bash
docker compose -f docker-compose.prod.yml stats
# Look for high MEMORY %
```

**Solutions:**
- Increase JVM heap:
  ```bash
  docker run -e JAVA_OPTS="-Xmx2g -Xms1g" ...
  ```
- Or in `docker-compose.prod.yml`:
  ```yaml
  backend:
    environment:
      JAVA_OPTS: "-Xmx2g -Xms1g"
  ```
- Restart: `docker compose restart backend`

### 3. Disk Space Full

**Symptoms:** Cannot write to database, file uploads fail

**Diagnosis:**
```bash
df -h                  # Check filesystem
docker exec postgres du -sh /var/lib/postgresql/data  # Database size
docker compose -f docker-compose.prod.yml exec backend du -sh /app/logs  # App logs
```

**Solutions:**
- Archive old logs: `gzip /app/logs/*.log`
- Prune Docker: `docker system prune -a`
- Expand volume size (specific to hosting platform)
- Clear old Sentry events

### 4. Slow Database Queries

**Symptoms:** High response times on list/item endpoints

**Diagnosis:**
```bash
# Enable slow query log in PostgreSQL
psql -U postgres -d inventory -c "ALTER SYSTEM SET log_min_duration_statement = 1000;"
psql -U postgres -d inventory -c "SELECT pg_reload_conf();"

# Check logs
docker compose -f docker-compose.prod.yml logs postgres | grep "duration:"
```

**Solutions:**
- Add indexes (see CLAUDE.md Database Migrations)
- Optimize N+1 queries: use `@EntityGraph` in repositories
- Implement caching: Redis or Spring Cache

### 5. Email Delivery Issues

**Symptoms:** Users don't receive verification emails, password resets fail

**Diagnosis:**
```bash
# Check Resend API quota
curl -H "Authorization: Bearer $RESEND_API_KEY" https://api.resend.com/emails

# Check logs
docker compose -f docker-compose.prod.yml logs backend | grep -i "email\|resend"
```

**Solutions:**
- Verify domain in Resend: https://resend.com/domains
- Check SPF/DKIM records
- Resend API quota: https://resend.com/settings/billing
- Consider using `EMAIL_PROVIDER=noop` in dev/staging

### 6. Stripe Webhook Failures

**Symptoms:** Payments processed but premium status not upgraded, "webhook signature mismatch"

**Diagnosis:**
```bash
# Check webhook events in Stripe
curl https://api.stripe.com/v1/events?limit=10 -u $STRIPE_SECRET_KEY

# Check logs for signature errors
docker compose -f docker-compose.prod.yml logs backend | grep -i "webhook\|signature"

# Verify webhook endpoint registered
curl https://dashboard.stripe.com/webhooks
```

**Solutions:**
- Regenerate webhook secret from Stripe dashboard: https://dashboard.stripe.com/webhooks
- Update `STRIPE_WEBHOOK_SECRET` in `.env.prod`
- Ensure endpoint URL is public and accessible
- Restart backend: `docker compose restart backend`
- Manually trigger webhook in Stripe dashboard to test

### 7. Image Upload / R2 Failures

**Symptoms:** "File validation error", images not stored, broken image URLs

**Diagnosis:**
```bash
# Check R2 connection
docker compose -f docker-compose.prod.yml exec backend curl -v https://$R2_ENDPOINT

# Check logs
docker compose -f docker-compose.prod.yml logs backend | grep -i "r2\|upload\|storage"

# Check R2 bucket policy
# https://dash.cloudflare.com → R2 → shelfio-images → Settings
```

**Solutions:**
- Verify R2 credentials: https://dash.cloudflare.com/profile/api-tokens
- Check bucket permissions (allow public read if needed)
- Test upload:
  ```bash
  aws s3 --endpoint-url $R2_ENDPOINT cp test.jpg s3://shelfio-images/ \
    --region auto --access-key $R2_ACCESS_KEY --secret-key $R2_SECRET_KEY
  ```
- In dev, use `STORAGE_PROVIDER=noop` (stores images in database)

### 8. AI Analysis (Gemini) Failures

**Symptoms:** "AI analysis service unavailable", analysis jobs stuck PENDING

**Diagnosis:**
```bash
# Check Gemini quota
curl -H "x-goog-api-key: $GEMINI_API_KEY" \
  https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent

# Check logs
docker compose -f docker-compose.prod.yml logs backend | grep -i "gemini\|analysis"

# Check rate limits
# https://console.cloud.google.com/apis/api/generativelanguage.googleapis.com/quotas
```

**Solutions:**
- Check Gemini API quota: https://aistudio.google.com/app/apikey
- Enable billing: https://console.cloud.google.com/billing
- Use `AI_PROVIDER=noop` to disable analysis (feature toggle)
- Increase timeout: `GEMINI_TIMEOUT=60` (default 30s)
- Increase thread pool: `GEMINI_THREAD_POOL=4` (default 2)
- Wait for quota reset or upgrade billing

---

## Scheduled Maintenance

### Daily

- Monitor error rates in Sentry
- Check disk space usage
- Verify backup completion (if applicable)

### Weekly

- Review slow query logs
- Check external service quotas (Stripe, Gemini, Resend)
- Test health checks
- Review failed email deliveries

### Monthly

- Run full backup
- Review and optimize database indexes
- Audit access logs for suspicious activity
- Update dependencies (if necessary)

### Quarterly

- Load test with production-like data
- Disaster recovery drill (restore from backup)
- Security audit (CORS, headers, token expiration)
- Update SSL/TLS certificate

---

## Backup & Recovery

### Database Backup (PostgreSQL)

**Manual backup:**

```bash
# Create backup
PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -U $DB_USERNAME -d $DB_NAME > backup.sql

# Compressed backup
PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -U $DB_USERNAME -d $DB_NAME | gzip > backup.sql.gz

# Backup size check
du -h backup.sql.gz
```

**Automated backup (cron):**

```bash
# Add to crontab (daily at 2 AM)
0 2 * * * /path/to/backup-script.sh
```

**Backup script example:**

```bash
#!/bin/bash
BACKUP_DIR=/backups
DB_NAME=inventory
DB_USER=postgres
DB_HOST=your-db-host

DATE=$(date +\%Y\%m\%d_\%H\%M\%S)
BACKUP_FILE=$BACKUP_DIR/inventory_$DATE.sql.gz

PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME | \
  gzip > $BACKUP_FILE

# Keep only last 30 days of backups
find $BACKUP_DIR -name "inventory_*.sql.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE"
```

### Restore from Backup

```bash
# Restore from uncompressed backup
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME < backup.sql

# Restore from compressed backup
gunzip < backup.sql.gz | PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME
```

### Image Backup (R2)

R2 offers automatic versioning. Enable in bucket settings:

1. Go to https://dash.cloudflare.com → R2 → shelfio-images → Settings
2. Enable "Object Versioning"
3. Set lifecycle rules (keep versions for 30 days)

To restore a deleted image:

```bash
# List object versions
aws s3api list-object-versions --bucket shelfio-images \
  --endpoint-url $R2_ENDPOINT \
  --access-key $R2_ACCESS_KEY \
  --secret-key $R2_SECRET_KEY

# Restore specific version
aws s3api copy-object --bucket shelfio-images --copy-source "shelfio-images/path/to/object?versionId=ABC123" \
  --key path/to/object \
  --endpoint-url $R2_ENDPOINT \
  --access-key $R2_ACCESS_KEY \
  --secret-key $R2_SECRET_KEY
```

---

## Scaling

### Horizontal Scaling (Load Balancer + Multiple Instances)

**1. Set up load balancer** (AWS ALB, Nginx, HAProxy):
- Route traffic to multiple backend instances
- Enable sticky sessions (for WebSocket if used)
- Health check on `GET /actuator/health`

**2. Deploy multiple backend replicas:**

```yaml
# docker-compose.prod.yml
services:
  backend-1:
    image: shelfio-backend:latest
    ports:
      - "8081:8080"
  backend-2:
    image: shelfio-backend:latest
    ports:
      - "8082:8080"
  backend-3:
    image: shelfio-backend:latest
    ports:
      - "8083:8080"

  nginx:
    image: nginx:latest
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
```

**3. Scale frontend** (CDN + multiple regions):
- Use Cloudflare or similar CDN
- Enable caching for static assets (CSS, JS, images)
- Reduce CORS issues by serving from same origin

### Vertical Scaling (Larger Instance)

1. **Increase JVM heap:**
   ```bash
   JAVA_OPTS="-Xmx4g -Xms2g" docker compose restart backend
   ```

2. **Increase database pool size:**
   ```
   spring.datasource.hikari.maximum-pool-size=30
   ```

3. **Increase thread pools:**
   ```
   GEMINI_THREAD_POOL=8
   OLLAMA_THREAD_POOL=4
   ```

---

## Secrets Management

### Rotate JWT Secret

```bash
# Generate new secret
NEW_SECRET=$(openssl rand -base64 32)

# Update in .env.prod
sed -i "s/JWT_SECRET=.*/JWT_SECRET=$NEW_SECRET/" .env.prod

# Restart backend (existing tokens remain valid until expiration)
docker compose -f docker-compose.prod.yml up -d
```

### Rotate Stripe Webhook Secret

1. In Stripe Dashboard (https://dashboard.stripe.com/webhooks):
   - Click endpoint → Signing secret → Reveal
   - Copy new secret

2. Update `.env.prod`:
   ```bash
   STRIPE_WEBHOOK_SECRET=whsec_xxxxx
   ```

3. Restart backend:
   ```bash
   docker compose -f docker-compose.prod.yml up -d
   ```

### Rotate Database Password

```bash
# 1. Change PostgreSQL password
psql -U postgres -h $DB_HOST -d $DB_NAME
\password postgres

# 2. Update .env.prod
sed -i "s/DB_PASSWORD=.*/DB_PASSWORD=$NEW_PASSWORD/" .env.prod

# 3. Restart backend
docker compose -f docker-compose.prod.yml up -d

# 4. Test connection
docker compose -f docker-compose.prod.yml logs backend | grep -i "datasource\|connection"
```

---

## Incident Response

### Service Down

**Step 1: Assess the situation**

```bash
# Check if services are running
docker compose -f docker-compose.prod.yml ps

# Check logs for errors
docker compose -f docker-compose.prod.yml logs backend frontend postgres
```

**Step 2: Check health endpoints**

```bash
curl -v http://localhost:8080/actuator/health
curl -v http://localhost:3000/
```

**Step 3: Restart affected services**

```bash
# Restart all
docker compose -f docker-compose.prod.yml restart

# Restart specific service
docker compose -f docker-compose.prod.yml restart backend
```

**Step 4: If issue persists, rollback**

```bash
# Stop current version
docker compose -f docker-compose.prod.yml down

# Deploy previous version
docker compose -f docker-compose.prod.yml up -d --pull always
```

**Step 5: Investigate root cause**

Review logs, check Sentry, check external service status pages.

### High Error Rate

**Step 1: Check error source**

```bash
# Backend errors
docker compose -f docker-compose.prod.yml logs backend | grep ERROR | tail -20

# Frontend errors (in Sentry or browser console)
```

**Step 2: Check external services**

- Stripe API status: https://status.stripe.com
- Google Gemini status: https://status.cloud.google.com
- Resend status: https://status.resend.com
- Cloudflare status: https://www.cloudflarestatus.com

**Step 3: Check quotas**

- Stripe: https://dashboard.stripe.com
- Gemini: https://aistudio.google.com/app/apikey → "Check quota"
- Resend: https://resend.com/settings/billing

**Step 4: Implement mitigation**

- Disable feature: Set `app.premium.enabled=false` to disable Stripe
- Disable AI: Set `AI_PROVIDER=noop` to disable Gemini
- Increase cache TTL: Reduce API calls
- Reduce rate limits temporarily: Prevent cascading failures

### Database Performance Degradation

```bash
# Check query performance
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d inventory -c "SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"

# Check index usage
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d inventory -c "SELECT * FROM pg_stat_user_indexes ORDER BY idx_scan ASC LIMIT 10;"

# Run VACUUM to optimize
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d inventory -c "VACUUM ANALYZE;"
```

---

## Useful Commands

```bash
# View all logs
docker compose -f docker-compose.prod.yml logs

# Follow backend logs only
docker compose -f docker-compose.prod.yml logs -f backend

# Execute command in container
docker compose -f docker-compose.prod.yml exec backend ls -la

# Open database shell
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U postgres -d inventory

# Get container IP
docker compose -f docker-compose.prod.yml exec backend hostname -I

# View resource usage
docker compose -f docker-compose.prod.yml stats

# Stop all services gracefully
docker compose -f docker-compose.prod.yml stop

# Force stop
docker compose -f docker-compose.prod.yml kill

# Remove all containers (keep volumes)
docker compose -f docker-compose.prod.yml down

# Remove everything including volumes
docker compose -f docker-compose.prod.yml down -v
```

---

## Support & Escalation

- **Code issues:** Review CLAUDE.md and CONTRIBUTING.md
- **API errors:** Check Sentry dashboard or backend logs
- **External service issues:** Check respective status pages and dashboards
- **Database issues:** Connect directly with `psql` and run diagnostics
- **Performance issues:** Monitor CPU/memory in `docker stats` and review slow query logs
