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
- **`service/mapper.kt`** — Parses xcontest HTML tables (Jsoup) into domain models. Handles two page layouts: world (`world=true`, pilot element has extra child) and Romania (`world=false`). Filters flights by takeoff country using `isTakeoffInRomania()` which checks for `flag_ro` CSS class on the takeoff column's flag `<span>`. Errors if the flag element is missing (HTML structure change detection).
- **`service/http/http.kt`** — HTTP client (Ktor/CIO). Uses a Browserless (headless Chrome) service to render JavaScript-heavy pages. Browserless URL is hardcoded in `http/constants.kt`.
- **`db/db.kt`** — Database layer using Jetbrains Exposed (DSL mode). Connects to PostgreSQL via env vars (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASS`, `DB_NAME`).
- **`db/gis.kt`** — Custom Exposed `ColumnType` for PostGIS `GEOGRAPHY(Point)` columns, bridging `org.postgis.Point` ↔ `PGgeography`.
- **`db/models.kt`** — Exposed table definitions: `flights`, `pilots`, `takeoffs`, `gliders`. Flights reference pilots, takeoffs (nullable), and gliders by FK.

### Key Details

- Flight times are parsed as local Bucharest time (`TZ` env var, defaults to `Europe/Bucharest`). The mapper produces `java.util.Date` (epoch millis), then `db.kt` converts via Joda-Time `DateTime` in the configured timezone for DB storage.
- PostGIS geography columns use GIST indexes for spatial queries on takeoff and flight start points.
- The `world` vs `romania` page distinction affects HTML structure: world pages have an extra country flag element before the pilot name.

## Environment Variables

### Application (Kotlin)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_HOST` | Yes | — | PostgreSQL host |
| `DB_PORT` | No | `5432` | PostgreSQL port |
| `DB_USER` | No | `xcontest` | PostgreSQL user |
| `DB_PASS` | Yes | — | PostgreSQL password |
| `DB_NAME` | No | `xcontest` | PostgreSQL database name |
| `TZ` | No | `Europe/Bucharest` | Timezone for flight time parsing |

### Playwright Content Service

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `PORT` | No | `3000` | HTTP server port |
| `HOST` | No | `0.0.0.0` | HTTP server bind address |
| `PROXY_SERVER` | No | — | Proxy URL (e.g., `https://rs.magneticproxy.net:443`) |
| `PROXY_USERNAME` | No | — | Proxy username (sticky session ID is appended automatically) |
| `PROXY_PASSWORD` | No | — | Proxy password |

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

| Browser mode | Chrome version | UA version | Proxy | Turnstile |
|---|---|---|---|---|
| `browserless/chrome:1-chrome-stable` (headless, v121) | 121 | — | No | **Fails** (error 600010) |
| `ghcr.io/browserless/chromium:latest` (headless, v145) | 145 | — | No | **Fails** (error 600010) |
| Headless + stealth flags (UA override, webdriver=false, etc.) | 145 | — | No | **Fails** |
| **Playwright headed + Xvfb** (`headless: false`) | 145 | 145 | No | **Passes in ~2s** |
| Playwright headed + Xvfb | 145 | **131 (mismatch!)** | Yes | **Fails** (error 600010) |
| **Playwright headed + Xvfb** | 145 | **145 (match)** | Yes | **Passes in ~6-8s** |

Two key requirements for passing Turnstile:
1. **`headless: false`** — running Chrome in headed mode with a virtual X display (`xvfb-run`) produces a browser fingerprint that passes Turnstile's challenge.
2. **User-Agent version must match the actual Chrome binary version.** When using a proxy, Turnstile applies stricter validation. A mismatch between the claimed UA version and the real browser version (detectable via TLS fingerprint/JS APIs) triggers error 600010. Without a proxy this mismatch is tolerated; with a proxy it is not.

### Playwright content service (drop-in Browserless replacement)

A drop-in replacement for Browserless lives at `playwright-content-service/`. It exposes the same `POST /content` API with `{url, waitFor}` body.

**How it handles hash URLs (pagination):**
1. Strips the hash fragment and loads the base URL first (so the initial API call hits the server cache and Turnstile can complete)
2. Waits up to 30s for the JWT to be set (Turnstile typically passes in ~6-8s through proxy)
3. Sets `window.location.hash` to trigger the paginated API call (which now succeeds with the JWT)
4. Waits for the pager to show the expected page number
5. Returns the rendered HTML

**Docker setup:**
```bash
docker build -t playwright-content playwright-content-service/
docker run -d -p 3000:3000 \
  -e PROXY_SERVER='https://proxy-host:443' \
  -e PROXY_USERNAME='customer-name-cc-us' \
  -e PROXY_PASSWORD='password' \
  playwright-content
```

**To use:** Point `BROWSERLESS_URL` in `http/constants.kt` to `http://<host>:3000/content`.

### Proxy support

The Playwright content service supports two proxy modes:

#### 1. Residential proxy (env var — for `populate` and `scrape` modes)

Configured via environment variables on the Docker container. Used for full scraping where Turnstile JWT is required.

| Variable | Required | Description |
|----------|----------|-------------|
| `PROXY_SERVER` | No | Proxy URL (e.g., `https://rs.magneticproxy.net:443`) |
| `PROXY_USERNAME` | No | Proxy username (e.g., `customer-name-cc-us`) |
| `PROXY_PASSWORD` | No | Proxy password |

**Sticky sessions:** Each browser instance appends `-sessid-<random>` to the proxy username (MagneticProxy format) to pin the same exit IP for the session duration. Without sticky sessions, rotating residential proxies assign different IPs per connection, which breaks Turnstile's IP correlation.

#### 2. Per-request free proxy (request body — for `recent` mode)

The `POST /content` API accepts an optional `proxy` field in the JSON body (e.g., `{"url":"...","waitFor":"...","proxy":"http://1.2.3.4:8080"}`). When set, it overrides any env var proxy for that request.

**JWT handling with free proxies:** When a per-request proxy is used and JWT is not obtained within 30s, the service proceeds without it (logs a warning but does not error). Turnstile CAN pass through free/datacenter proxies (confirmed via Tor Browser testing), but success depends on the specific proxy. The **first page (offset 0) is server-cached** and works without JWT.

#### Free proxy rotation in `fetchRecent()`

When `USE_FREE_PROXY=true` is set, `fetchRecent()` in `Xcontest2Db.kt`:
1. Scrapes HTTPS-capable proxies from `free-proxy-list.net` (via `Http.fetchFreeProxies()`)
2. Shuffles the list randomly
3. Tries each proxy in turn, calling the Playwright content service with `{"proxy":"http://host:port"}` in the request body
4. Validates the proxy using the **Romania URL first** (reliable — no hash-based country filter needed)
5. If Romania returns flights, processes them, then tries the **world URL as best-effort** (world URL failure doesn't block Romania data)
6. Stops on the first proxy that returns real flight data
7. If all proxies fail, logs an error

**Note:** `BROWSERLESS_URL` must point to the Playwright content service (not the old browserless) when using free proxies, since only the Playwright service supports per-request proxy.

**Critical: User-Agent must match Chrome version.** The User-Agent strings in `server.mjs` **MUST** declare the same Chrome major version as the actual Chromium binary in the Docker image. A version mismatch (e.g., claiming Chrome 131 when the binary is Chrome 145) is a strong automation signal that causes Cloudflare Turnstile to fail with error 600010, even in headed mode. When upgrading the Playwright Docker image, always update the UA strings to match:
```bash
# Check the Chrome version in the image:
docker run --rm --entrypoint bash <image> -c '/ms-playwright/chromium-*/chrome-linux64/chrome --version'
```

### The `populate()` + `scrape()` flow

`populate()` creates date-based scrape tasks and `scrape()` processes them, using a **two-level loop** (dates × pages) to bypass the 1000-item pagination cap:

1. For each base URL (year/league), loads the overview page
2. Extracts available dates from the date filter `<select>` dropdown (option values in `YYYY-MM-DD` format)
3. For each date, constructs a date-filtered URL and loads the first page
4. Gets `lastPageOffset` from `.pg-edge` links (`null` → 0 for single-page results)
5. Processes the first page, then paginates remaining pages at `step 100`

**URL construction patterns:**
- Romania per-date: `$url#filter[date]=$date`
- Romania per-date paged: `$url#filter[date]=$date@flights[start]=$offset`
- World per-date: `$url#flights[sort]=reg@filter[country]=RO@filter[date]=$date`
- World per-date paged: `$url#flights[sort]=reg@filter[country]=RO@filter[date]=$date@flights[start]=$offset`

**Why date splitting is needed:** xcontest.org enforces a client-side `maxItems: 1000` limit. With 100 flights per page, max offset is 900 (10 pages). Years/leagues with >1000 flights silently lose data without date splitting.

### Date filter details

- Hash parameter format: `filter[date]=YYYY-MM-DD` (e.g., `filter[date]=2025-09-30`)
- The date dropdown is a `<select>` element rendered by JS in the filter form, with `<option value="YYYY-MM-DD">` entries
- `extractAvailableDates()` finds the first `<select>` whose options match the `\d{4}-\d{2}-\d{2}` pattern
- The `@` character separates multiple hash parameters (not `&`)

### Playwright content service: date filter handling

For date-filter hashes (no `start]` offset), the service:
1. Captures table row count before setting the hash
2. Sets `window.location.hash` to the date-filtered hash
3. Waits for the table row count to change (confirms filtered data has rendered)
4. Applies 2s settle time for final DOM updates

### Known issue: hash handling race condition for non-paginated hashes

For hashes without `start]` (e.g., country filter `filter[country]=RO@flights[sort]=reg`), the Playwright service's wait logic has a weakness:

1. The first `waitForFunction` checks for `start]` in the hash — **no match → returns `true` immediately** (designed for pagination, not filters)
2. The fallback `waitForFunction` waits for `tbody.children.length !== prevCount` — but if both unfiltered and filtered pages have ~100 rows, the row count **never changes**, so this **times out after 30s** and `.catch()` silently swallows the error
3. After 2s settle, the content is returned

**In practice this usually works**: the filtered API call completes in 1-5s, so by the time the 30s timeout fires, the table has already been showing correct filtered data for ~25s. But if the API call fails for any reason (403, network error, slow proxy), the table still shows the initial unfiltered data, and the service returns it without error.

**Defense-in-depth**: `isTakeoffInRomania()` in `mapper.kt` filters by takeoff country flag (`flag_ro`) regardless of whether the server-side country filter was applied. This prevents non-Romanian flights from being persisted even if the Playwright service returns unfiltered data.

### Static asset caching (bandwidth optimization)

The Playwright content service uses **application-level route caching** to avoid re-downloading static CDN assets on every browser launch. Without this, a full scrape (~150 page loads) would re-download ~700 MB of identical static assets because Chrome's built-in HTTP cache is partitioned by proxy credentials (each launch gets a new sticky session ID → different cache partition).

**How it works:** `page.route('**/*', ...)` intercepts all requests. For cacheable CDN domains, responses are stored in an in-memory `Map` in the Node.js process. On cache hit, `route.fulfill()` serves the cached response directly without any network call. On cache miss, `route.fetch()` gets the response from the network, caches it, then fulfills.

**Cached domains** (configured in `CACHEABLE_DOMAINS`):
- `unpkg.com` — JS library CDN (immutable)
- `d393ilck4xazzy.cloudfront.net` — CloudFront CDN assets
- `s.xcontest.app` / `s.xcontest.org` — xcontest static assets
- `static.cloudflareinsights.com` / `cloudflareinsights.com` — analytics

**NOT cached:**
- `www.xcontest.org` — dynamic flight data and API responses
- `challenges.cloudflare.com` — Turnstile challenge responses are dynamic/session-specific; caching them breaks JWT verification

**Measured impact** (tested with residential proxy, each request gets fresh proxy session + UA):

| Request | Cache hits | Cache misses | Bandwidth fetched |
|---------|-----------|-------------|-------------------|
| 1st (cold) | 0 | 54 | ~1,871 KB |
| 2nd (warm) | 55 | 31 | ~1 KB |
| 3rd+ (warm) | 84 | 2 | ~25 KB |

Projected savings for a full 150-request scrape: ~700 MB of CDN traffic (unpkg, CloudFront, xcontest static) reduced to a one-time ~5 MB download. Turnstile traffic (~312 MB) still goes through the proxy since it cannot be cached. **Estimated ~70% total bandwidth reduction.**

**What's preserved:** proxy session rotation (new IP per browser launch), User-Agent rotation, Turnstile challenge flow — all unchanged. The cache only affects static CDN assets that are identical across all requests.

**Why not Chrome's built-in cache:** Chrome partitions its HTTP disk cache by proxy credentials. Since each browser launch uses a different sticky session username (e.g., `customer-name-sessid-abc123`), Chrome treats each launch as a different proxy and refuses to reuse cached resources. This was confirmed by testing `launchPersistentContext` with a shared user data directory — 0 cache hits with proxy, 38 cache hits without proxy on the same directory. The `--disk-cache-dir` Chrome flag also doesn't work with Playwright's ephemeral profiles (cache index created but no data stored).

## Tech Stack

- Kotlin 2.0 / JVM 21, Gradle with Shadow plugin (fat JAR)
- Ktor Client (CIO engine) for HTTP
- Jsoup for HTML parsing
- Jetbrains Exposed (DSL) for database access
- PostgreSQL + PostGIS
- Playwright content service (headed Chrome + Xvfb) for JS-rendered pages, replacing Browserless

# Bugs

When fixing a bug, always attempt to add a test case that first reproduces the easy, and only then fix it. Include HTML fixture where parsing is involved. Use a temporary/mock database when the database is involved.