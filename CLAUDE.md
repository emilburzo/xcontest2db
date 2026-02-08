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

## Build & Deploy

**CI/CD:** GitHub Actions (`.github/workflows/docker.yml`) triggers on push to `master`:
1. Builds with Gradle (`./gradlew build`) using JDK 21
2. Builds a multi-arch Docker image (`linux/amd64`, `linux/arm64`) via Buildx
3. Pushes to DockerHub as `emilburzo/xcontest2db` with tags: `latest`, run number, and short SHA

**Docker image:** `eclipse-temurin:21-jre-alpine` base, runs the fat JAR (`./gradlew shadowJar`).

**Deployment:** Runs as a Kubernetes CronJob. DockerHub credentials are stored as GitHub Actions secrets (`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`).

## xcontest.org Scraping: How It Works & Gotchas

### Page rendering pipeline

xcontest.org is a JavaScript-heavy SPA. The server returns a shell HTML page, then client-side JS fetches flight data from an internal API and renders the table.

1. Page loads → JS calls `XContest.run()` which triggers a `fetch()` to `/api/data/?flights/<league>/<year>&lng=<lang>&key=<API_KEY>&list[start]=0&list[num]=100&list[sort]=time_claim&list[dir]=down`
2. The API returns JSON (`{"list":{"startItemIndex":0,"numberItemsRequested":100,"numberItemsReturned":100,"numberItems":899},"items":[...]}`)
3. The JS framework renders the flight table (`.XClist`) from this JSON
4. Pagination uses **hash fragments** (`#flights[start]=200`), not query parameters. Hash changes trigger new API calls for subsequent pages.

The API key (`XContest.API_KEY`) is static across the site: `03ECF5952EB046AC-A53195E89B7996E4-D1B128E82C3E2A66`.

### Cloudflare Turnstile protection

xcontest.org uses **Cloudflare Turnstile** (invisible mode) to protect the data API:

1. On page load, `userVerify()` (from `/js/user-verify.js`) renders a Turnstile widget (siteKey `0x4AAAAAACB9q_ruxqlbdcYE`, container `#tw`)
2. On successful challenge, the Turnstile token is POSTed to `/api/auth/user-verify/?inv`
3. The server returns a **JWT**
4. The JWT is stored via `XContest.updateOptions('flights', { verifyToken: jwt })` and included in subsequent API requests
5. Without a valid JWT, the API returns **403 Forbidden**

**The initial API call for `list[start]=0` succeeds without the JWT** because the server has a **file cache** for it (response header: `X-Cache-Params: CACHE-HIT=Y.fresh-cachehit`). All other offsets require the JWT.

### Why headless Chrome fails

Cloudflare Turnstile detects headless Chrome and refuses to issue a token. This was tested with:

| Browser mode | Chrome version | Turnstile |
|---|---|---|
| `browserless/chrome:1-chrome-stable` (headless, v121) | 121 | **Fails** (error 600010) |
| `ghcr.io/browserless/chromium:latest` (headless, v145) | 145 | **Fails** (error 600010) |
| Headless + stealth flags (UA override, webdriver=false, etc.) | 145 | **Fails** |
| **Playwright headed + Xvfb** (`headless: false`) | 145 | **Passes in ~2s** |

The key differentiator is `headless: false` — running Chrome in headed mode with a virtual X display (`xvfb-run`) produces a browser fingerprint that passes Turnstile's PAT (Proof of Attestation Token) challenge.

### Playwright content service (drop-in Browserless replacement)

A drop-in replacement for Browserless lives at `playwright-content-service/`. It exposes the same `POST /content` API with `{url, waitFor}` body.

**How it handles hash URLs (pagination):**
1. Strips the hash fragment and loads the base URL first (so the initial API call hits the server cache and Turnstile can complete)
2. Waits up to 20s for the JWT to be set (Turnstile typically passes in ~2s)
3. Sets `window.location.hash` to trigger the paginated API call (which now succeeds with the JWT)
4. Waits for the pager to show the expected page number
5. Returns the rendered HTML

**Docker setup:**
```bash
docker build -t playwright-content playwright-content-service/
docker run -d -p 3000:3000 playwright-content \
  sh -c "xvfb-run --auto-servernum --server-args='-screen 0 1280x1024x24' node server.mjs"
```

**To use:** Point `BROWSERLESS_URL` in `http/constants.kt` to `http://<host>:3000/content`.

### The `fetchAll()` flow

`Xcontest2Db.fetchAll()` iterates over base URLs for each year/league (Romania + World), finds the last page offset via `.pg-edge` links, then loops through all pages at `step 100`. Each page URL is `"$baseUrl#flights[start]=$offset"`. The HTML is parsed by `mapFlights()` using Jsoup, and new flights are deduplicated and persisted.

## Tech Stack

- Kotlin 2.0 / JVM 21, Gradle with Shadow plugin (fat JAR)
- Ktor Client (CIO engine) for HTTP
- Jsoup for HTML parsing
- Jetbrains Exposed (DSL) for database access
- PostgreSQL + PostGIS
- Playwright content service (headed Chrome + Xvfb) for JS-rendered pages, replacing Browserless
