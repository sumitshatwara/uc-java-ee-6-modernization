# DayTrader Spring Boot scaffold

This is an independent Maven project (`io.daytrader:daytrader-boot`) using Java 21 and Spring Boot 3.3. It is intentionally not a module of the legacy root POM.

## Layout

- `io.daytrader.entity` contains the Jakarta Persistence entities and preserves the legacy table/column names.
- `io.daytrader.repository` contains Spring Data repositories.
- `io.daytrader.service.TradeService` is the constructor-injected, transactional business-service port.
- `io.daytrader.web` contains the `/app` front controller, handler extension point, shared renderer, and session keys.
- `src/main/resources/templates` contains the shared Thymeleaf shell; `static/` holds the legacy UI assets.
- `src/main/resources/db/ddl` contains H2 and PostgreSQL schemas.

## Adding a slice

Implement a Spring bean that implements `ActionHandler`, advertises its action names, and renders a template in `templates/`. Keep the shared `TradeAppController`, `PageRenderer`, and common templates stable; a slice should add its own handler and view rather than editing shared dispatch or navigation files. The controller rejects duplicate action names during application startup.

## Build, run, seed, and compare

```bash
make boot-build
make boot-run
curl -s http://localhost:8080/daytrader/actuator/health
curl -i http://localhost:8080/rest/unknown
make parity TARGET=http://localhost:8080/daytrader
make boot-stop
```

`make boot-run` starts the executable jar in the background, writes `work/boot.log` and `work/boot.pid`, then calls the shared seed script. The local database is H2 in PostgreSQL mode; activate the `postgres` Spring profile and provide `SPRING_DATASOURCE_*` variables for PostgreSQL. `boot-run` starts the app with `-Duser.timezone=UTC`.

If Maven Central rate-limits the build, set `MVN_SETTINGS=/path/to/settings.xml` for either boot target. The settings file is external to this repository and must not be committed.
