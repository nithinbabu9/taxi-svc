# Taxi API Bruno collection

The standalone API contract lives at `../docs/openapi.yaml`. Runtime Swagger UI is
available at `http://localhost:8080/swagger-ui.html`.

Open this `bruno` directory in Bruno and select the **Local** environment.

Run requests in numerical order. The first two Users requests register both travelers
and save their user IDs and JWT access tokens in the Local environment. The optional
login requests issue fresh tokens. Ride, match, and chat requests automatically use the
correct traveler's bearer token, so no UUID or token copying is required.

For fully offline local testing, start the application with `--spring.profiles.active=local`.
This uses deterministic fixture coordinates and makes no Google requests. The **Cancel
Ride 1** and **Reject Match** requests demonstrate alternative terminal flows; do not run
them before completing the acceptance/chat flow.

The collection uses unique email addresses on each run and creates departure timestamps
24 hours in the future.
