# ---- Stage 1: Build frontend ----
FROM node:20-alpine AS frontend-build
WORKDIR /app
ARG VITE_GOOGLE_CLIENT_ID
ARG VITE_SENTRY_DSN
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID
ENV VITE_SENTRY_DSN=$VITE_SENTRY_DSN
COPY src/frontend/package.json src/frontend/package-lock.json ./
RUN npm ci
COPY src/frontend/ .
RUN npm run build

# ---- Stage 2: Build backend ----
FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /app
COPY src/backend/.mvn .mvn
COPY src/backend/mvnw src/backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src/backend/src src
RUN ./mvnw package -DskipTests

# ---- Stage 3: Runtime ----
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache nginx gettext libwebp-tools

WORKDIR /app

# Backend JAR
COPY --from=backend-build /app/target/*.jar app.jar

# Frontend static files
COPY --from=frontend-build /app/dist /usr/share/nginx/html

# Nginx config template
RUN mkdir -p /etc/nginx/templates
COPY src/frontend/nginx.conf.template /etc/nginx/templates/default.conf.template

# Start script
COPY start.sh start.sh
RUN chmod +x start.sh

CMD ["./start.sh"]
