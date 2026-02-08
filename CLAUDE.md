# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**xcontest2db** — A Kotlin batch job that scrapes paragliding flight data from [xcontest.org](https://www.xcontest.org) and persists it to a PostgreSQL/PostGIS database. Runs as a Kubernetes CronJob every 19 minutes.

## Build & Test Commands

```bash
./gradlew build          # Build + run tests
./gradlew test           # Run tests only
./gradlew shadowJar      # Build fat JAR (output: build/libs/xcontest2db-1.0-SNAPSHOT-all.jar)
./gradlew run            # Run the application (requires DB and Browserless env vars)
```

Test framework: JUnit (via `kotlin("test-junit")`). Single test class: `FlightMapperTest`.

## Architecture

The app follows a three-layer pipeline: **HTTP fetch → HTML parse/map → DB persist**.

### Layers

- **`main/Main.kt`** — Entry point. Wires `Db` and `Http` into `Xcontest2Db`, calls `fetchRecent()`.
- **`service/Xcontest2Db.kt`** — Orchestrator. Fetches flight list pages, deduplicates against existing DB records, and persists new flights with their related entities (pilot, takeoff, glider) using get-or-create pattern.
- **`service/mapper.kt`** — Parses xcontest HTML tables (Jsoup) into domain models. Handles two page layouts: world (`world=true`, pilot element has extra child) and Romania (`world=false`).
- **`service/http/http.kt`** — HTTP client (Ktor/CIO). Uses a Browserless (headless Chrome) service to render JavaScript-heavy pages. Browserless URL is hardcoded in `http/constants.kt`.
- **`db/db.kt`** — Database layer using Jetbrains Exposed (DSL mode). Connects to PostgreSQL via env vars (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASS`, `DB_NAME`).
- **`db/gis.kt`** — Custom Exposed `ColumnType` for PostGIS `GEOGRAPHY(Point)` columns, bridging `org.postgis.Point` ↔ `PGgeography`.
- **`db/models.kt`** — Exposed table definitions: `flights`, `pilots`, `takeoffs`, `gliders`. Flights reference pilots, takeoffs (nullable), and gliders by FK.

### Key Details

- Flight times are parsed as local Bucharest time (`TZ` env var, defaults to `Europe/Bucharest`). The mapper produces `java.util.Date` (epoch millis), then `db.kt` converts via Joda-Time `DateTime` in the configured timezone for DB storage.
- PostGIS geography columns use GIST indexes for spatial queries on takeoff and flight start points.
- The `world` vs `romania` page distinction affects HTML structure: world pages have an extra country flag element before the pilot name.

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_HOST` | Yes | — | PostgreSQL host |
| `DB_PORT` | No | `5432` | PostgreSQL port |
| `DB_USER` | No | `xcontest` | PostgreSQL user |
| `DB_PASS` | Yes | — | PostgreSQL password |
| `DB_NAME` | No | `xcontest` | PostgreSQL database name |
| `TZ` | No | `Europe/Bucharest` | Timezone for flight time parsing |

## Tech Stack

- Kotlin 2.0 / JVM 21, Gradle with Shadow plugin (fat JAR)
- Ktor Client (CIO engine) for HTTP
- Jsoup for HTML parsing
- Jetbrains Exposed (DSL) for database access
- PostgreSQL + PostGIS
- Browserless (external headless Chrome service) for JS-rendered pages
