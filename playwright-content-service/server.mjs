import http from 'http';
import { chromium } from 'playwright';

const PORT = process.env.PORT || 3000;

const server = http.createServer(async (req, res) => {
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

  const { url, waitFor, waitForSelector, gotoOptions } = params;
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
    browser = await chromium.launch({
      headless: false,
      args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-blink-features=AutomationControlled'],
    });
    const page = await browser.newPage();

    // Always load the base URL first (without hash) so the initial
    // API call hits the server cache and Turnstile can complete.
    await page.goto(baseUrl, { waitUntil: 'domcontentloaded', timeout });

    if (selector) {
      await page.waitForSelector(selector, { timeout });
    }

    if (hasHash) {
      // Wait for Turnstile JWT to be set (up to 20s)
      let jwtReady = false;
      for (let i = 0; i < 20; i++) {
        const jwt = await page.evaluate(() => {
          try { return XContest?.gadgets?.flights?.View?.options?.verifyToken; }
          catch { return null; }
        }).catch(() => null);
        if (jwt) { jwtReady = true; break; }
        await new Promise(r => setTimeout(r, 1000));
      }

      if (!jwtReady) {
        console.warn(`JWT not obtained after 20s for ${url}`);
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

server.listen(PORT, () => console.log(`Playwright content service listening on port ${PORT}`));
