# Production deployment

This service is packaged as a container and expects configuration through environment variables.
Never copy a local `.env` file or any credentials into an image or source control.

## Build and run the container

```bash
docker build -t wayfare-api:latest .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL='jdbc:postgresql://YOUR_DB_HOST:5432/taxi?sslmode=require' \
  -e DATABASE_USERNAME=wayfare_app \
  -e DATABASE_PASSWORD='replace-me' \
  -e GOOGLE_MAPS_API_KEY='replace-me' \
  -e JWT_SECRET='replace-with-a-long-random-secret' \
  -e SMTP_HOST=smtp.gmail.com \
  -e SMTP_PORT=587 \
  -e SMTP_USERNAME='sender@gmail.com' \
  -e SMTP_PASSWORD='google-app-password' \
  -e SMTP_FROM='sender@gmail.com' \
  wayfare-api:latest
```

## Cloud Run with Cloud SQL

For Cloud Run, use the Cloud SQL Java connector rather than the Docker Compose `database`
hostname. Attach the Cloud SQL instance to the Cloud Run service and configure:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql:///taxi?socketFactory=com.google.cloud.sql.postgres.SocketFactory&cloudSqlInstance=PROJECT_ID:REGION:INSTANCE_ID&ipTypes=PUBLIC
DATABASE_USERNAME=APPLICATION_DATABASE_USER
DATABASE_PASSWORD=<Secret Manager secret>
DATABASE_MAXIMUM_POOL_SIZE=3
```

The Cloud Run service account needs the **Cloud SQL Client** role. Store the database password,
JWT secret, Google Maps key, and SMTP credentials in Secret Manager rather than plain environment
variables.

The production profile disables Swagger and OpenAPI HTTP endpoints, exposes only health
information under `/actuator/health`, enables graceful shutdown, and enables a per-instance
authentication rate limiter.

For local Docker Desktop development, use the repository's Compose module instead:

```bash
cp docker-compose.env.example .env
docker compose up --build
```

## Required infrastructure

Use a managed PostgreSQL instance with automated backups and point-in-time recovery. Create a
least-privileged application role which owns the schema or has the permissions required by
Flyway migrations. Run the service behind an HTTPS-only load balancer or reverse proxy; do not
publish the container's port directly to the internet.

Store every value above in the hosting platform's secret manager. Use separate databases, Google
Maps keys, JWT secrets, and SMTP credentials for staging and production. Restrict the Google Maps
server key to the deployed backend/network and required APIs, then configure quota and billing
alerts.

For mail delivery at scale, use a transactional provider and configure SPF, DKIM, and DMARC for
the sender domain. Gmail SMTP and an App Password are suitable only for initial testing and have
operational limits.

## Health checks and rollout

Configure the platform health check to call `/actuator/health/readiness`. Deploy migrations as
part of application startup; Flyway validates and applies only new versioned migrations. Take a
database backup before the first production deployment and before every schema-affecting release.

The in-application rate limiter is deliberately limited to a single process. Configure durable,
distributed rate limits at the CDN, API gateway, or load balancer before running multiple
application replicas.
