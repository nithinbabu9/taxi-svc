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

## GitHub Actions continuous deployment

The repository includes `.github/workflows/ci-cd.yml`. Pull requests into `main` run the Maven
test suite. A successful push to `main` builds a tagged container image, publishes it to Artifact
Registry, then deploys it to the existing Cloud Run service.

The workflow uses GitHub OpenID Connect and Google Workload Identity Federation. Do **not** create
or upload a long-lived Google service-account key to GitHub.

### One-time Google Cloud setup

Run the following commands from Cloud Shell. They intentionally trust only pushes from the `main`
branch of this repository.

```bash
export PROJECT_ID='taxi-local-507119'
export PROJECT_NUMBER='10006969213'
export GITHUB_REPOSITORY='nithinbabu9/taxi-svc'
export DEPLOY_SA='wayfare-github-deploy'
export RUNTIME_SA='wayfare-run'
export POOL_ID='github-actions-pool'
export PROVIDER_ID='github-actions-provider'

gcloud services enable iamcredentials.googleapis.com sts.googleapis.com --project="$PROJECT_ID"

gcloud iam service-accounts create "$DEPLOY_SA" \
  --project="$PROJECT_ID" \
  --display-name='Wayfare GitHub Actions deployer'

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${DEPLOY_SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role='roles/artifactregistry.writer'

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${DEPLOY_SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role='roles/run.admin'

gcloud iam service-accounts add-iam-policy-binding \
  "${RUNTIME_SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --member="serviceAccount:${DEPLOY_SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role='roles/iam.serviceAccountUser'

gcloud iam workload-identity-pools create "$POOL_ID" \
  --project="$PROJECT_ID" \
  --location='global' \
  --display-name='GitHub Actions'

gcloud iam workload-identity-pools providers create-oidc "$PROVIDER_ID" \
  --project="$PROJECT_ID" \
  --location='global' \
  --workload-identity-pool="$POOL_ID" \
  --display-name='Wayfare GitHub provider' \
  --issuer-uri='https://token.actions.githubusercontent.com' \
  --attribute-mapping='google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner,attribute.ref=assertion.ref' \
  --attribute-condition="assertion.repository == '${GITHUB_REPOSITORY}' && assertion.ref == 'refs/heads/main'"

gcloud iam service-accounts add-iam-policy-binding \
  "${DEPLOY_SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="$PROJECT_ID" \
  --role='roles/iam.workloadIdentityUser' \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL_ID}/attribute.repository/${GITHUB_REPOSITORY}"
```

### GitHub repository variables

In GitHub, open **Settings → Secrets and variables → Actions → Variables**, then add:

| Variable | Value |
| --- | --- |
| `GCP_PROJECT_ID` | `taxi-local-507119` |
| `GCP_REGION` | `us-east5` |
| `GAR_LOCATION` | `us-central1` |
| `GAR_REPOSITORY` | `wayfare` |
| `CLOUD_RUN_SERVICE` | `wayfare-api` |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | `wayfare-github-deploy@taxi-local-507119.iam.gserviceaccount.com` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/10006969213/locations/global/workloadIdentityPools/github-actions-pool/providers/github-actions-provider` |

Create a GitHub environment named `production` under **Settings → Environments**. Add required
reviewers there if deployment approval is desired. Cloud Run configuration and Secret Manager
references already on `wayfare-api` are retained by image-only deployments; do not put database,
SMTP, JWT, or Google Maps secrets in GitHub variables.
