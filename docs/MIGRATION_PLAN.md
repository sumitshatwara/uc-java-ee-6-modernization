# DayTrader migration plan

This plan carries the recorded Liberty baseline forward without changing the legacy application or its parity transcripts. The target is the Java 21 / Spring Boot application under `daytrader-boot/`, with AWS deployment resources added separately.

## Ordered phases

| Phase | Work | Parity coverage |
| --- | --- | --- |
| 0. Baseline | Build and seed the unchanged Liberty application; retain its 13 recorded scenarios and known quirks as the reference. | All scenarios against `http://localhost:9083/daytrader`. |
| 1. Inventory and scaffold | Map the 146 legacy estate rows to replacements; create the standalone Spring Boot application, shared web shell, persistence model, seed endpoint, health endpoint, and build/test wiring. | Smoke tests and the existing 13 scenarios, with differences expected for slices not yet ported. |
| 2. Per-slice migration | Fan out the independently owned application slices below. Each slice adds its own `ActionHandler` and templates, using the shared controller and renderer. | Run each slice's listed scenario IDs against a seeded app; never change the Liberty transcripts to hide differences. |
|  | **Authentication and registration:** login, logout, register, session state and error handling. | `01-login`, `04-register`, `09-invalid-login`, `10-logout` |
|  | **Market and home:** dashboard summary, quote lookup, and home-page account summary. | `02-home`, `03-quotes` |
|  | **Trading:** synchronous buy/sell, order completion/cancellation, holdings and order alerts. | `05-buy`, `06-sell`, `07-portfolio` |
|  | **Account:** account display and profile update. | `08-account-update` |
|  | **REST:** address list, search, and entry lookup under the legacy `/rest` path. | `11-rest-list`, `12-rest-search`, `13-rest-entry` |
|  | **Tests:** port relevant legacy unit tests and add focused repository, service, MVC, and persistence tests. | Unit and integration tests; preserve scenario-level arithmetic checks. |
|  | **Infrastructure:** add the ECS, ALB, network, RDS, IAM, secrets, and logging Terraform roots; validate and plan only. | Infrastructure validation; no deployment or `terraform apply`. |
| 3. Integration | Integrate completed slices on `migration/daytrader-boot-verify`; resolve shared-file conflicts, run the full container build, and compare the seeded application with Liberty. | Full 13-scenario container parity plus service tests. |
| 4. Recorded verification | Run side-by-side checks against the recorded Liberty baseline, review intentional differences and operational evidence, and document any approved behavior decisions. | Publish the parity report and comparison evidence; transcripts remain immutable. |

## Replacement map by concern

| Legacy concern | Target |
| --- | --- |
| IBM deployment bindings and extensions | Remove `ibm-*.xml`; inject Spring beans, keep runtime configuration in `application.yml`, use an ALB listener, and preserve transaction isolation in Hikari configuration. |
| JNDI JDBC resources and persistence unit | Spring Data JPA / Hibernate 6 over `spring.datasource.*` and HikariCP; Aurora PostgreSQL in containers/ECS, H2 in PostgreSQL mode for local runs and tests. |
| EJB business services | Port `TradeSLSBBean` behavior to constructor-injected `TradeService` (`@Service`, `@Transactional`); retain only the JPA runtime path. |
| JMS factories, destinations, MDBs | Synchronous order processing remains the default. An optional `sqs` profile maps broker work to SQS and streamer events to SNS→SQS listeners. No embedded broker is carried forward. |
| EAR modules and Liberty runtime | One executable Spring Boot fat jar containing `io.daytrader.service`, `io.daytrader.entity`, the web UI, and REST API; embedded Tomcat on port 8080 behind an ALB. |
| JSP/servlet UI | Thymeleaf templates and Spring MVC handlers keep the existing `/daytrader/app?action=…` URLs and visible text/form contract. |
| JAX-RS sample | Spring MVC `@RestController`; the Tomcat rewrite valve and ALB path rule route `/rest/**` into the `/daytrader` context. |
| J2C credentials and wsadmin resources | Aurora credentials arrive through Secrets Manager-backed ECS task secrets; SQS/SNS access uses the ECS task IAM role. Maven dependencies are packaged in the fat jar. |
| Liberty / tWAS deployment operations | ECS Fargate task and service, ALB target group, CloudWatch logs, SSM parameters, and managed Aurora PostgreSQL/SQS/SNS resources. `legacy-ops/wsadmin` is synthetic and is not runtime evidence. |

## Risks and constraints

- Derby/Db2 DDL differs from PostgreSQL. Convert and validate the schema explicitly; keep `KEYGENEJB` as a table-backed allocator with the legacy `KEYNAME`/`KEYVAL` block semantics rather than silently replacing its IDs.
- Quote prices change randomly. Parity masks volatile amounts and checks cash/holding arithmetic, so preserve the checks instead of comparing random values literally.
- JSF pages are outside the parity scenarios and are dropped; do not treat them as migrated UI.
- The MQ-related wsadmin material is synthetic and has never run against a real queue manager. The target is managed SQS/SNS, not an assertion of existing IBM MQ runtime use.
- `java.util.Date#toString()` embeds a time-zone abbreviation. Run parity JVMs with `TZ=UTC` / `-Duser.timezone=UTC` so date text uses a stable two-to-five-letter zone.
- `keygenejb` allocation, lazy JPA relationships, and large seed writes can affect portability and seed time; exercise them against both H2 PostgreSQL mode and PostgreSQL before production use.

## Decisions

- Keep JSP pages' `/daytrader/app?action=…` URLs while porting their UI to Thymeleaf. Drop the JSF pages; they are not covered by parity.
- Keep synchronous order processing as the default. SQS/SNS integration is optional and disabled unless the `sqs` profile is active.
- Do not port the Direct JDBC runtime mode; use the JPA implementation only.
- Preserve the recorded quirks unless a later decision is written down: invalid login produces HTTP 500, the account page reports `Total Orders: 0`, and the REST entry endpoint returns HTTP 415.
- Use H2 in PostgreSQL mode for local development and tests; use `spring.datasource.*` with PostgreSQL/Aurora for containers and ECS.
- There are no WebSphere-proprietary API imports in the legacy application. The `com.ibm.websphere.samples.daytrader` package is application code and is renamed to `io.daytrader.*` as it is ported.
