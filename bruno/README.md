# Taxi API Bruno collection

The standalone API contract lives at `../docs/openapi.yaml`. Runtime Swagger UI is
available at `http://localhost:8080/swagger-ui.html`.

Open this `bruno` directory in Bruno and select the **Local** environment.

Run requests in numerical order. Successful create requests automatically save IDs in
the Local environment so subsequent requests work without copying UUIDs manually.

For fully offline local testing, start the application with `--spring.profiles.active=local`.
This uses deterministic fixture coordinates and makes no Google requests. The **Cancel
Ride 1** and **Reject Match** requests demonstrate alternative terminal flows; do not run
them before completing the acceptance/chat flow.

The collection uses unique email addresses on each run and creates departure timestamps
24 hours in the future.
