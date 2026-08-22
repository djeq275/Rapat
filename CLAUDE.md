# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a **greenfield Spring Boot project** — currently just the Spring Initializr scaffold (`Vibe1Application` + one generated test), no domain code yet. The product requirements live in [project_scope.md](project_scope.md): a meeting-scheduling app for a company where Ketua Divisi schedule meetings, Direktur is auto-invited to every meeting, meetings sync to Google Calendar, and meeting minutes ("notulensi") are written under per-meeting granular permissions rather than global roles. Read `project_scope.md` before implementing any feature — it's the source of truth for actors (Admin, Direktur, Ketua Divisi, Karyawan) and functional scope.

## Commands

- Build: `./gradlew build`
- Run the app: `./gradlew bootRun`
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests "com.example.vibe1.Vibe1ApplicationTests"`
- Run a single test method: `./gradlew test --tests "com.example.vibe1.Vibe1ApplicationTests.contextLoads"`

No linter/formatter is configured in `build.gradle`.

## Architecture

- **Java 25** via Gradle toolchain, **Gradle Groovy DSL** (`build.gradle`, not Kotlin DSL).
- **Spring Boot MVC + Thymeleaf**: server-rendered pages, not a REST/SPA backend.
- **Spring Modulith** (`spring-modulith-starter-core`/`-jpa`): new business logic should be organized as top-level packages under `com.example.vibe1.*` (e.g. `meeting`, `division`, `user`), each acting as an application module whose boundaries Modulith verifies — avoid reaching into another module's internal classes directly.
- **Liquibase** handles schema migrations; **MariaDB** + **Spring Data JPA** is the persistence layer.
- **Lombok** is available for entities/DTOs (`compileOnly`/`annotationProcessor` configured).
- RBAC is required per `project_scope.md`, but note: meeting-minutes write access is a **per-meeting grant** (a Ketua Divisi/Admin designates a specific karyawan as notulis for one meeting), not a global role — model it as a relation (e.g. a `meeting_notetaker` join), not another entry in the role hierarchy.
- Google Calendar sync and Google OAuth login are functional requirements from `project_scope.md` but no corresponding dependencies exist in `build.gradle` yet — they'll need to be added (Spring Security OAuth2 Client, Google Calendar API client) when that work starts.

## Local database / Docker Compose

- `compose.yaml` defines a `mariadb` service with its own literal credentials (`myuser`/`secret`, root `verysecret`, db `mydatabase`). Because `spring-boot-docker-compose` is a `developmentOnly` dependency, running `./gradlew bootRun` **auto-starts this compose service and wires the datasource automatically** — no manual `docker compose up` or datasource config needed for local dev.
- `.env` / `.env.example` at the repo root hold a *separate* set of placeholder DB/Google OAuth credentials for documentation purposes. **They are not currently wired into Spring's configuration** (nothing in `application.properties` reads them, and Spring's docker-compose integration reads `compose.yaml` directly, not `.env`). Don't assume changing `.env` affects the running app until this is actually connected (e.g. via `spring-dotenv` or by parameterizing `compose.yaml` with `${VAR}` substitution).
- `.env` is gitignored; `.env.example` is the committed template.
