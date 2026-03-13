#!/bin/sh
set -e

# Redirect all output to stdout so Railway captures everything
exec 2>&1

echo "=== Container starting ==="
echo "Date: $(date -u)"
echo "Memory: $(free -m 2>/dev/null || echo 'free not available')"
echo "Disk: $(df -h /app 2>/dev/null || echo 'df not available')"

# Railway assigns PORT dynamically
PORT=${PORT:-3000}
BACKEND_URL="http://127.0.0.1:8080"
export PORT BACKEND_URL

echo "PORT=$PORT, BACKEND_URL=$BACKEND_URL"
echo "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-not set}"
echo "DB_HOST=${DB_HOST:+set}, DB_PORT=${DB_PORT:-5432}, DB_NAME=${DB_NAME:+set}"

# Generate nginx config from template
envsubst '${PORT} ${BACKEND_URL}' < /etc/nginx/templates/default.conf.template > /etc/nginx/http.d/default.conf
echo "=== Generated nginx config ==="
cat /etc/nginx/http.d/default.conf
echo "=== End nginx config ==="

# Start Spring Boot backend on internal port 8080
echo "Starting Java process..."
java -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC \
  -Dserver.port=8080 \
  -Dmanagement.server.port=8081 \
  -jar /app/app.jar &
BACKEND_PID=$!

# Wait for backend to be healthy (up to 120 seconds)
echo "Waiting for backend to start (PID: $BACKEND_PID)..."
READY=false
for i in $(seq 1 60); do
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    wait "$BACKEND_PID" 2>/dev/null
    EXIT_CODE=$?
    echo "Backend process exited unexpectedly with code $EXIT_CODE"
    exit 1
  fi
  HEALTH_RESPONSE=$(wget -qO- http://127.0.0.1:8081/actuator/health 2>&1)
  if [ $? -eq 0 ]; then
    echo "Backend is ready. Health: $HEALTH_RESPONSE"
    READY=true
    break
  fi
  echo "Health check attempt $i/60: $HEALTH_RESPONSE"
  sleep 2
done

if [ "$READY" != "true" ]; then
  echo "Backend failed to start within 120 seconds."
  exit 1
fi

# Verify backend is also responding on the main app port
echo "Verifying backend on port 8080..."
APP_RESPONSE=$(wget -qO- http://127.0.0.1:8080/api/v1/auth/me 2>&1) || true
echo "Port 8080 response: $APP_RESPONSE"

# Forward shutdown signals
trap "kill $BACKEND_PID 2>/dev/null; kill $NGINX_PID 2>/dev/null" TERM INT

# Start nginx in foreground
echo "Starting nginx on port $PORT..."
nginx -g "daemon off;" &
NGINX_PID=$!

echo "=== Application started on port $PORT ==="

# Background monitor: check backend health every 30 seconds
while true; do
  sleep 30
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "[MONITOR] Backend process (PID: $BACKEND_PID) is DEAD at $(date -u)"
    wait "$BACKEND_PID" 2>/dev/null
    echo "[MONITOR] Backend exit code: $?"
    break
  fi
  MONITOR_HEALTH=$(wget -qO- http://127.0.0.1:8081/actuator/health 2>&1) || true
  echo "[MONITOR] $(date -u) - Backend PID $BACKEND_PID alive - Health: $MONITOR_HEALTH"
done &

# Wait for nginx (main process for container lifecycle)
wait $NGINX_PID
