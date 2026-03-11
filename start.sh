#!/bin/sh
set -e

# Railway assigns PORT dynamically
PORT=${PORT:-3000}
BACKEND_URL="http://127.0.0.1:8080"
export PORT BACKEND_URL

# Generate nginx config from template
envsubst '${PORT} ${BACKEND_URL}' < /etc/nginx/templates/default.conf.template > /etc/nginx/http.d/default.conf

# Start Spring Boot backend on internal port 8080
java -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC \
  -Dserver.port=8080 \
  -Dmanagement.server.port=8081 \
  -jar /app/app.jar &
BACKEND_PID=$!

# Wait for backend to be healthy (up to 120 seconds)
echo "Waiting for backend to start..."
READY=false
for i in $(seq 1 60); do
  if wget -qO- http://127.0.0.1:8081/actuator/health >/dev/null 2>&1; then
    echo "Backend is ready."
    READY=true
    break
  fi
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "Backend process exited unexpectedly." >&2
    exit 1
  fi
  sleep 2
done

if [ "$READY" != "true" ]; then
  echo "Backend failed to start within 120 seconds." >&2
  exit 1
fi

# Forward shutdown signals to backend process
trap "kill $BACKEND_PID 2>/dev/null" TERM INT

# Start nginx in foreground
nginx -g "daemon off;" &
NGINX_PID=$!

echo "Application started on port $PORT"

# Wait for nginx (main process for container lifecycle)
wait $NGINX_PID
