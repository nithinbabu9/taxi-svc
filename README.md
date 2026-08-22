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

The browser interface walks through the complete two-traveler flow: user creation, ride
planning, match review and acceptance, chat creation, and messaging. It stores generated
UUIDs in browser local storage so the workflow survives a page refresh. Existing users
are reused by email, and **Start another booking** clears trip state while retaining both
traveler accounts.

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

## Match acceptance and chat

Both matched users must accept before a chat is created. Until authentication is added,
the acting user's UUID is supplied in the request body:

```text
POST /api/matches/{matchId}/accept
POST /api/matches/{matchId}/reject

{"userId":"<participant UUID>"}
```

The second acceptance atomically marks the match and rides as matched, cancels competing
pending matches, creates one chat, and adds both users as participants. Match rows are
locked during decisions, and database uniqueness constraints prevent duplicate chats.

```text
GET  /api/users/{userId}/chats
GET  /api/chats/{chatId}
GET  /api/chats/{chatId}/messages?page=0&size=50
POST /api/chats/{chatId}/messages

{"senderId":"<participant UUID>","message":"See you at the pickup point"}
```

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.0/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.0/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.0/reference/web/servlet.html)
* [Spring Security](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-security.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.0/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.1.0/reference/actuator/index.html)
* [Anthropic Claude](https://docs.spring.io/spring-ai/reference/api/chat/anthropic-chat.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.
