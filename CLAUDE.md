# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Meeting-scheduling app for a company: Ketua Divisi schedule meetings, Direktur is auto-invited to every meeting, meetings sync to Google Calendar with real invites, and meeting minutes ("notulensi") are written under per-meeting granular permissions rather than global roles. Requirements live in [project_scope.md](project_scope.md) and [PRD.md](PRD.md) — read them before changing functional scope. [GitHub issue #1](https://github.com/djeq275/vibe1/issues/1) tracks the implementation as 8 stages; this codebase implements all of them (branch `feature/issue-1-meeting-scheduling`).

## Commands

- Build: `./gradlew build`
- Run the app: `docker compose up -d` (or just `./gradlew bootRun`, which auto-starts it) then `./gradlew bootRun`
- Run all tests: `./gradlew test` (needs a working Docker daemon -- several tests use Testcontainers against a real MariaDB image)
- Run a single test class: `./gradlew test --tests "com.example.vibe1.meeting.MeetingAccessServiceTest"`
- Run a single test method: `./gradlew test --tests "com.example.vibe1.meeting.MeetingAccessServiceTest.karyawanCanViewOwnDivisionMeeting"`

No linter/formatter is configured in `build.gradle`.

### Spring Boot 4 test annotation packages

This project is on Spring Boot 4.1 / Spring Framework 7, which relocated common test annotations. If a class doesn't compile, check the package first instead of assuming the classic Boot 2/3 location:
- `@WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (not `org.springframework.boot.test.autoconfigure.web.servlet`)
- `@MockitoBean` (replaces `@MockBean`) → `org.springframework.test.context.bean.override.mockito.MockitoBean`
- Test-scope starters are per-slice now (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`, etc.), not one monolithic `spring-boot-starter-test`.
- Testcontainers is on the 2.x line; module artifacts are prefixed (`org.testcontainers:testcontainers-junit-jupiter`, `org.testcontainers:testcontainers-mariadb`, not the old unprefixed `junit-jupiter`/`mariadb`).

## Architecture

- **Java 25** via Gradle toolchain, **Gradle Groovy DSL** (`build.gradle`, not Kotlin DSL).
- **Spring Boot MVC + Thymeleaf**: server-rendered pages, not a REST/SPA backend. Shared layout via `templates/fragments/layout.html` (`head`/`nav` fragments), Bootstrap via CDN, no frontend build tooling.
- **Spring Modulith**: top-level packages under `com.example.vibe1.*` are modules; `ModularityTests.verify()` enforces the graph is acyclic. Current allowed edges: `security → user`, `meeting → {division, user}`, `calendar → {meeting, user}`, `security → calendar` (the home page's "connect Google Calendar" banner). **`meeting` must never depend on `security`** — that would close a 3-module cycle (`security → calendar → meeting → security`); this is why `meeting.web.MeetingController` reads the current user via plain `java.security.Principal` + `user.UserRepository` instead of `security.UserPrincipal`. Sub-packages like `*.web` hold controllers/DTOs internal to their module.
  - **`spring-modulith-observability-api`/`-core` are deliberately NOT dependencies** (removed after a real bug): that module's `ModuleEntryInterceptor` CGLIB-proxies every bean in every module for tracing, and that broke `GoogleOidcUserService` at runtime with `AOP configuration seems to be invalid: tried calling method [...loadUser...]` — a known Spring/CGLIB interaction where a proxied class implementing a *parameterized generic* interface (`OAuth2UserService<OidcUserRequest, OidcUser>`) gets a compiler-generated bridge method the proxy's method dispatch resolves incorrectly. This is latent for any bean implementing a generic interface, not just this one, and only manifests when the real Spring-managed bean is actually invoked (unit tests that `new` the class directly, or web tests that mock it, never hit it) — so if module-level tracing is ever added back, re-verify with a real OAuth2 login, not just `ModularityTests`/mocked slice tests.
- **Liquibase** (`src/main/resources/db/changelog/`, one numbered file per changeset, included from `db.changelog-master.xml`) is the only way schema changes happen; Hibernate runs in `ddl-auto=validate`, never `update`/`create` in the committed config.
  - **When adding/changing a JPA entity**, don't hand-write the Liquibase column types from the annotations. Hibernate's own schema-generation-to-script feature will tell you the *exact* DDL it expects (this already caught a real bug: `@Lob` + `@Column(name=...)` without an explicit `columnDefinition` silently mapped to `tinytext` instead of `longtext` on MariaDB). Temporarily add to `application.properties`:
    ```
    spring.jpa.hibernate.ddl-auto=none
    spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create
    spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=/tmp/schema-export.sql
    spring.jpa.properties.jakarta.persistence.schema-generation.create-source=metadata
    ```
    then run `./gradlew test --tests "com.example.vibe1.Vibe1ApplicationTests"` (it boots a real Testcontainers MariaDB), read `/tmp/schema-export.sql`, transcribe the relevant `create table`/`alter table` lines into a new Liquibase changeset, then revert `application.properties` back to `ddl-auto=validate` and rerun the test to confirm it validates clean.
- **spring.jpa.open-in-view=false** — lazy `@ManyToOne` associations (e.g. `Meeting.division`/`Meeting.organizer`) are **not** available after a repository method returns. Controllers that render them must use a repository method with an explicit `JOIN FETCH` (see `MeetingRepository.findDetailById`/`findAllByOrderByStartTimeDesc`), not the plain `findById`.
- **`@EnableJpaAuditing` lives in `common.JpaAuditingConfig`, not on `Vibe1Application`.** Putting it on the `@SpringBootApplication` class breaks any `@WebMvcTest` slice (which still uses that class as its `@SpringBootConfiguration` source) with "JPA metamodel must not be empty", since the slice has no `EntityManagerFactory`.
- RBAC (Admin/Direktur/Ketua Divisi/Karyawan) gates coarse access (`@PreAuthorize("hasRole(...)")`); division-scoped visibility and per-meeting notulensi permission are **not** expressible as static roles and live in explicit services instead: `meeting.MeetingAccessService` (division-scoped view), `meeting.NotulensiAccessService` (per-meeting write/manage, backed by the `meeting_notetaker` relation — not a global role). `MeetingAccessService.assertCanView` is called on the direct `/meetings/{id}` GET, not just the list query, to close the IDOR gap of a Karyawan guessing another division's meeting id.
- Google Calendar integration:
  - Two OAuth2 client registrations (`google-login`: identity for everyone; `google-calendar`: `calendar.events` scope, only ever requested from organizer-capable roles — see `User.isOrganizerCapable()` / `calendar.CalendarConsentStatus`).
  - Tokens for `google-calendar` are encrypted at rest (AES-256-GCM, `calendar.TokenCryptoConverter`, key from `GOOGLE_TOKEN_ENCRYPTION_KEY`) in a custom `OAuth2AuthorizedClientService` (`calendar.GoogleAuthorizedClientService`) — deliberately not Spring's built-in `JdbcOAuth2AuthorizedClientService`, which stores tokens plaintext. `google-login` stays in the default HTTP-session store. `calendar.RegistrationRoutingAuthorizedClientRepository` is what routes between the two per registration id.
  - `meeting.MeetingService.create(...)` persists the `Meeting` and publishes `meeting.MeetingScheduledEvent` via Spring's `ApplicationEventPublisher`; because `spring-modulith-starter-jpa` is on the classpath, that publish is durably recorded in the `event_publication` table in the same transaction (Modulith's event publication registry) — this is the retry-safety net, no separate queue. `calendar.MeetingCalendarSyncListener` (`@ApplicationModuleListener`) consumes it and calls `.setSendUpdates("all")` when inserting the Calendar event — **do not drop that flag**, it's the actual fix for the "Direktur never gets notified" bug this project exists to solve.
  - Sync failure sets `Meeting.calendarSyncStatus = FAILED` (with the error message) and does **not** retry automatically; a human retries via `POST /meetings/{id}/retry-sync`, served by `calendar.web.CalendarSyncController` — that controller lives in the `calendar` package even though its route is nested under `/meetings/**`, specifically so `meeting` never has to depend on `calendar` (see the module-graph note above).

## Local database / Docker Compose / secrets

- `.env` (gitignored) holds real local secrets; `.env.example` is the committed template — keep both in sync when adding a new required variable.
- `compose.yaml`'s `mariadb` service reads `${DB_NAME}/${DB_USERNAME}/${DB_PASSWORD}/${DB_ROOT_PASSWORD}/${DB_PORT}` from `.env` via Docker Compose's native `.env`-file substitution (no extra tooling). **`DB_USERNAME` must not be `root`** — the official MariaDB image ignores/rejects `MARIADB_USER=root` since that account already exists.
- `spring-boot-docker-compose` (`developmentOnly`) auto-starts that service and wires `spring.datasource.*` from the running container when you run `./gradlew bootRun` — there's no explicit datasource config in `application.properties` for this path.
- The `bootRun` Gradle task (see `build.gradle`) loads `.env` into its own JVM process environment so `application.properties`' `${GOOGLE_OAUTH_CLIENT_ID}`-style placeholders resolve too. This is **not** wired into the `test` task — tests stay hermetic and use Testcontainers for their own database.
- **Bootstrap admin account**: there is no self-registration (`security.GoogleOidcUserService` never auto-creates a `User`; `security.SecurityConfig`'s form login only works for existing rows). The only way into a fresh database is the seed row in `db/changelog/004-seed-bootstrap-admin.xml` — `admin@company.local` / `ChangeMe123!`. **Log in and change this immediately after any real deployment**; it's a known/public credential in this repo's history.
- `GOOGLE_TOKEN_ENCRYPTION_KEY` must be a base64-encoded 32-byte value (`openssl rand -base64 32`). Losing/rotating it makes every stored `google-calendar` token unreadable — affected users just have to reconnect via the banner on the home page.
- **Running the app as a container** (not `bootRun`): `./runlocal.sh` rebuilds the `meeting:1` image (`./gradlew bootBuildImage`, Cloud Native Buildpacks — no Dockerfile) and runs `docker compose up --force-recreate`, which now also starts a `meeting` app service alongside `mariadb`. That service activates Spring profile `docker` (`application-docker.properties`), which sets `spring.datasource.*` explicitly from env vars — deliberately **not** added to the main `application.properties`, since doing so would make Boot back off from the `spring-boot-docker-compose` auto-wiring that `./gradlew bootRun` relies on. The two datasource paths (bootRun auto-wiring vs. the `docker` profile) are intentionally separate and don't interact.
- `compose.yaml` also runs a `phpmyadmin` service (pointed at `mariadb` via `PMA_HOST`/`PMA_PORT`) at http://localhost:8081 for poking at the database directly — log in with any of the `.env` DB credentials (`DB_USERNAME`/`DB_PASSWORD` for the app user, or `root`/`DB_ROOT_PASSWORD`). Local dev convenience only, not started in any deployment path.
