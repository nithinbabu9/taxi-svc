# Getting Started

Taxi is a Spring Boot ride-sharing and travel-matching application. It resolves trip
locations, evaluates route compatibility, coordinates two-sided match acceptance, and
creates a private chat for matched travelers. The repository also includes a responsive
browser UI, interactive Swagger documentation, and a Bruno API collection.

## Repository layout

```text
taxi/
├── bruno/                         Shareable API request collection
├── docs/openapi.yaml              Standalone OpenAPI contract
├── src/main/java/uber/taxi/
│   ├── client/                    Google and offline map providers
│   ├── config/                    Maps, matching, security, and OpenAPI config
│   ├── controller/                REST controllers
│   ├── dto/                       API request and response models
│   ├── entity/                    JPA domain entities and enums
│   ├── exception/                 Application errors and global handler
│   ├── mapper/                    Entity-to-DTO mapping
│   ├── repository/                Spring Data repositories
│   ├── service/                   Ride, matching, acceptance, and chat logic
│   └── util/                      Distance and polyline utilities
├── src/main/resources/
│   ├── db/migration/              Versioned Flyway migrations
│   └── static/                    Browser UI (HTML, CSS, JavaScript)
├── src/test/                      Unit and Spring context tests
├── .env.example                  Environment variable template
├── pom.xml                       Maven build
└── README.md                     Project documentation
```

## Local web interface

Start the application with the `local` profile, then open:

```text
http://localhost:8080
```

The browser interface walks through Gmail registration, six-digit email verification, login,
ride planning, match review and acceptance, chat creation, and messaging. It stores the access
token in browser local storage so the workflow survives a page refresh.

## Docker Compose

To start the application and an isolated PostgreSQL database from Docker Desktop:

```bash
cp docker-compose.env.example .env
docker compose up --build
```

Open `http://localhost:8080`. The default Docker setup uses the `local` profile, so map data is
offline and the email verification code appears in the application container logs:

```bash
docker compose logs -f app
```

Use `docker compose down` to stop the stack. The `taxi-postgres-data` named volume preserves
your local Docker database; use `docker compose down -v` only when you intentionally want a
fresh database.

## Bruno API collection

Open the repository's `bruno` directory as a collection in Bruno and select the `Local`
environment. Run the numbered requests in order. Create requests capture generated UUIDs
automatically for the following requests. See `bruno/README.md` for workflow notes.

## Swagger / OpenAPI

With the application running, open the interactive documentation at:

```text
http://localhost:8080/swagger-ui.html
```

The machine-readable contract is available as JSON at `/v3/api-docs` and YAML at
`/v3/api-docs.yaml`. Swagger's **Try it out** feature can call the local API directly.

## Local configuration

For offline development without a Google key, run:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

In IntelliJ, configure `local` as the active Spring profile and provide database
credentials through `DATABASE_USERNAME` and `DATABASE_PASSWORD` environment variables.

The local profile uses deterministic fixture coordinates and routes. They are intended
only for API workflow testing and are not real navigation results.

The application requires PostgreSQL and Google Maps Platform credentials. Enable the
Geocoding API and Routes API in the Google Cloud project, then provide configuration
through environment variables:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/taxi
export DATABASE_USERNAME=taxi
export DATABASE_PASSWORD=your-local-password
export GOOGLE_MAPS_API_KEY=your-restricted-server-key
export JWT_SECRET=replace-with-a-32-byte-or-longer-random-secret
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=your-sender@gmail.com
export SMTP_PASSWORD=your-google-app-password
export SMTP_FROM=your-sender@gmail.com
```

Restrict the Google key to the required APIs and to the backend's deployment environment.
The key is sent in request headers and is never logged.

Creating or updating a ride geocodes both addresses and computes a driving route. The
API stores Google's formatted addresses, place IDs, coordinates, route distance, route
duration, and overview encoded polyline. Flyway applies the database schema migrations.

## Ride matching

Creating a ride automatically evaluates other users' open rides within the configured
departure-time window. Matching uses direct pickup proximity, pickup-to-route proximity,
destination proximity, decoded-polyline overlap, and departure-time difference. Retrieve
ranked results with:

```text
GET /api/rides/{rideId}/matches
```

Thresholds and score weights are configured under `app.matching` in `application.yaml`.
Changing a ride refreshes its pending matches; cancelling a ride cancels its pending
matches. Canonical ride ordering and a database unique constraint prevent duplicate pairs.

## Authentication, match acceptance, and chat

Registration creates an unverified account and sends a six-digit code. Verify that code before
logging in or receiving a bearer token:

```text
POST /api/auth/register
POST /api/auth/verify-email
POST /api/auth/resend-verification
POST /api/auth/login
GET  /api/users/me
```

All ride, match, and chat endpoints require `Authorization: Bearer <accessToken>`. The
server uses the token's subject to determine the acting user; callers cannot choose another
user by putting a UUID in the request body.

For local development, the verification code is written to the application log and is never
sent externally. Outside the `local` profile, configure SMTP through the environment variables
above. A Gmail sender must use a [Google App Password](https://myaccount.google.com/apppasswords),
not the sender's normal Gmail password. Do not commit SMTP credentials.

Both matched users must accept before a chat is created:

```text
POST /api/matches/{matchId}/accept
POST /api/matches/{matchId}/reject
```

The second acceptance atomically marks the match and rides as matched, cancels competing
pending matches, creates one chat, and adds both users as participants. Match rows are
locked during decisions, and database uniqueness constraints prevent duplicate chats.

```text
GET  /api/chats/mine
GET  /api/chats/{chatId}
GET  /api/chats/{chatId}/messages?page=0&size=50
POST /api/chats/{chatId}/messages

{"message":"See you at the pickup point"}
```
