import http from 'http';
import crypto from 'crypto';
import { chromium } from 'playwright';

const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || '0.0.0.0';
const PROXY_SERVER = process.env.PROXY_SERVER || null;
const PROXY_USERNAME = process.env.PROXY_USERNAME || null;
const PROXY_PASSWORD = process.env.PROXY_PASSWORD || null;

/**
 * Generate a proxy username with a sticky session ID.
 * Residential proxies rotate IPs per connection by default, which breaks
 * Cloudflare Turnstile (it correlates challenge IP with verification IP).
 * Adding `-sessid-<id>` pins the same exit IP for the session duration.
 */
function proxyUsernameWithSession(baseUsername) {
  if (!baseUsername) return null;
  const sessId = crypto.randomBytes(8).toString('hex');
  return `${baseUsername}-sessid-${sessId}`;
}

/**
 * Detect the actual Chromium version by launching a throwaway headless instance.
 * Returns the major version string (e.g. "145.0.0.0").
 */
async function detectChromiumVersion() {
  let browser;
  try {
    browser = await chromium.launch({ headless: false, args: ['--no-sandbox', '--disable-dev-shm-usage'] });
    const version = browser.version(); // e.g. "145.0.7632.6"
    return version.split('.')[0] + '.0.0.0';
  } finally {
    if (browser) await browser.close().catch(() => {});
  }
}

// User-Agents are built dynamically at startup to match the actual Chromium binary.
let USER_AGENTS = [];

function buildUserAgents(chromeVersion) {
  return [
    `Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${chromeVersion} Safari/537.36`,
    `Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${chromeVersion} Safari/537.36`,
    `Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${chromeVersion} Safari/537.36`,
  ];
}

function randomUserAgent() {
  return USER_AGENTS[Math.floor(Math.random() * USER_AGENTS.length)];
}

// Cache health check result to avoid launching a browser on every probe
let lastHealthCheck = { time: 0, ok: false };
const HEALTH_CACHE_MS = 60_000;

async function checkBrowserHealth() {
  const now = Date.now();
  if (now - lastHealthCheck.time < HEALTH_CACHE_MS) {
    return lastHealthCheck.ok;
  }
  let browser;
  try {
    browser = await chromium.launch({ headless: false, args: ['--no-sandbox', '--disable-dev-shm-usage'] });
    const page = await browser.newPage();
    const response = await page.goto('https://www.google.com/robots.txt', { timeout: 30_000 });
    lastHealthCheck = { time: now, ok: response?.ok() };
    return lastHealthCheck.ok;
  } catch (err) {
    console.error('Health check failed:', err.message);
    lastHealthCheck = { time: now, ok: false };
    return false;
  } finally {
    if (browser) await browser.close().catch(() => {});
  }
}

const server = http.createServer(async (req, res) => {
  // Health check endpoint for K8s probes — launches a real browser to verify it works
  if (req.method === 'GET' && req.url === '/health') {
    const ok = await checkBrowserHealth();
    res.writeHead(ok ? 200 : 503, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: ok ? 'ok' : 'browser_unavailable' }));
    return;
  }

  if (req.method !== 'POST' || !req.url.startsWith('/content')) {
    res.writeHead(404);
    res.end('Not found');
    return;
  }

  let body = '';
  for await (const chunk of req) body += chunk;

  let params;
  try {
    params = JSON.parse(body);
  } catch {
    res.writeHead(400);
    res.end('Invalid JSON');
    return;
  }

  const { url, waitFor, waitForSelector, gotoOptions, proxy } = params;
  if (!url) {
    res.writeHead(400);
    res.end('Missing "url"');
    return;
  }

  const selector = waitForSelector || waitFor || null;
  const timeout = gotoOptions?.timeout || 60_000;

  // Parse URL to detect hash fragment
  const hashIndex = url.indexOf('#');
  const hasHash = hashIndex !== -1;
  const baseUrl = hasHash ? url.substring(0, hashIndex) : url;
  const hash = hasHash ? url.substring(hashIndex + 1) : null;

  let browser;
  try {
    const launchOptions = {
      headless: false,
      args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-blink-features=AutomationControlled'],
    };
    // Per-request proxy (from request body) takes precedence over env var proxy.
    // Per-request proxies are plain http://host:port with no auth (e.g. free proxies).
    if (proxy) {
      launchOptions.proxy = { server: proxy };
    } else if (PROXY_SERVER) {
      const sessionUsername = proxyUsernameWithSession(PROXY_USERNAME);
      launchOptions.proxy = { server: PROXY_SERVER };
      if (sessionUsername) launchOptions.proxy.username = sessionUsername;
      if (PROXY_PASSWORD) launchOptions.proxy.password = PROXY_PASSWORD;
    }
    const userAgent = randomUserAgent();
    browser = await chromium.launch(launchOptions);
    const page = await browser.newPage({ userAgent });

    // Always load the base URL first (without hash) so the initial
    // API call hits the server cache and Turnstile can complete.
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded', timeout });

    if (selector) {
      await page.waitForSelector(selector, { timeout });
    }

    if (hasHash) {
      // Wait for Turnstile JWT to be set.
      // With residential proxies / non-blocked IPs, Turnstile passes and JWT is obtained.
      // With free/datacenter proxies, Turnstile fails — but the first page (offset 0)
      // is server-cached and works without JWT, so we continue anyway.
      const jwtTimeout = 30;
      let jwtReady = false;
      for (let i = 0; i < jwtTimeout; i++) {
        const jwt = await page.evaluate(() => {
          try { return XContest?.gadgets?.flights?.View?.options?.verifyToken; }
          catch { return null; }
        }).catch(() => null);
        if (jwt) { jwtReady = true; break; }
        await new Promise(r => setTimeout(r, 1000));
      }

      if (!jwtReady && !proxy) {
        throw new Error(`JWT not obtained after ${jwtTimeout}s for ${url}`);
      }
      if (!jwtReady) {
        console.warn(`JWT not obtained for ${url} (proxy mode) — proceeding without it`);
      }

      // Capture row count before hash change to detect table re-render
      const rowCountBefore = await page.evaluate(() => {
        const tbody = document.querySelector('#flights > table > tbody');
        return tbody ? tbody.children.length : 0;
      }).catch(() => 0);

      // Now navigate to the hash URL — the page JS will use the JWT for the API call
      await page.evaluate((h) => { window.location.hash = h; }, hash);

      // Wait for the content to update
      if (selector) {
        // Wait for the table to re-render by watching for row number change
        await page.waitForFunction(
          (h) => {
            // Check if the pager shows we're on the right page
            const match = h.match(/start\]=(\d+)/);
            if (!match) return true;
            const offset = parseInt(match[1]);
            const expectedPage = Math.floor(offset / 100) + 1;
            const strong = document.querySelector('.XCpager strong');
            return strong && parseInt(strong.textContent) === expectedPage;
          },
          hash,
          { timeout: 30_000 }
        ).catch(() => {
          console.warn(`Pager did not update for hash ${hash}`);
        });

        // For date filter changes (no start] offset), wait for the table to re-render
        if (!hash.match(/start\]/)) {
          await page.waitForFunction(
            (prevCount) => {
              const tbody = document.querySelector('#flights > table > tbody');
              if (!tbody) return false;
              // Table has re-rendered when row count changes or content is present
              return tbody.children.length > 0 && tbody.children.length !== prevCount;
            },
            rowCountBefore,
            { timeout: 30_000 }
          ).catch(() => {
            console.warn(`Table did not re-render for date filter hash ${hash}`);
          });
        }
      }

      // Small settle time for DOM updates
      await new Promise(r => setTimeout(r, 2000));
    } else {
      // No hash — just wait a bit for initial render to settle
      await new Promise(r => setTimeout(r, 1000));
    }

    const content = await page.content();
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(content);
  } catch (err) {
    console.error(`Error processing ${url}:`, err.message);
    res.writeHead(500);
    res.end(err.message);
  } finally {
    if (browser) await browser.close().catch(() => {});
  }
});

(async () => {
  try {
    const chromeVersion = await detectChromiumVersion();
    USER_AGENTS = buildUserAgents(chromeVersion);
    console.log(`Detected Chromium version: ${chromeVersion}`);
  } catch (err) {
    console.error('Failed to detect Chromium version, using fallback:', err.message);
    USER_AGENTS = buildUserAgents('130.0.0.0');
  }

  server.listen(PORT, HOST, () => {
    console.log(`Playwright content service listening on port ${PORT}`);
    if (PROXY_SERVER) {
      console.log(`Proxy configured: ${PROXY_SERVER}`);
    }
  });
})();
