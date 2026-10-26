#!/usr/bin/env node
// Checks a stamped web distribution in headless Chromium, served the way Cloudflare Pages would
// serve it: the _headers file (including the CSP) applies, and missing files get 404.html.
//
//   python tools/stamp_web_dist.py <dist>
//   NODE_PATH="$(npm root -g)" node tools/check-web-dist.cjs <dist> [--dev]
//
// --dev allows 'unsafe-eval' for a development build, whose webpack bundle uses eval().
// It checks: the loading screen shows and then gives way to the app, no CSP violations or page
// errors, the service worker caches every file, a reload works offline, and the "too old" and
// "could not start" screens appear when they should. Needs Playwright with Chromium.
'use strict';

const fs = require('fs');
const http = require('http');
const path = require('path');
const { chromium } = require('playwright');

const args = process.argv.slice(2);
const dist = args.find((arg) => !arg.startsWith('--'));
const dev = args.includes('--dev');
if (!dist || !fs.existsSync(path.join(dist, 'index.html'))) {
  console.error('usage: node tools/check-web-dist.cjs <stamped dist folder> [--dev]');
  process.exit(2);
}

const TYPES = {
  '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.css': 'text/css',
  '.wasm': 'application/wasm', '.json': 'application/json', '.webp': 'image/webp',
  '.png': 'image/png', '.svg': 'image/svg+xml', '.webmanifest': 'application/manifest+json',
  '.ttf': 'font/ttf', '.otf': 'font/otf',
};

// The subset of Cloudflare's _headers syntax this project uses: exact paths, "/*" and "/*.ext".
function headerRules() {
  const rules = [];
  let current = null;
  for (const line of fs.readFileSync(path.join(dist, '_headers'), 'utf8').split('\n')) {
    if (!line.trim() || line.startsWith('#')) continue;
    if (!/^\s/.test(line)) {
      current = { pattern: line.trim(), headers: [] };
      rules.push(current);
    } else {
      const colon = line.indexOf(':');
      current.headers.push([line.slice(0, colon).trim(), line.slice(colon + 1).trim()]);
    }
  }
  return rules;
}

function matches(pattern, urlPath) {
  if (pattern === '/*' || pattern === urlPath) return true;
  return pattern.startsWith('/*.') && urlPath.endsWith(pattern.slice(2));
}

function serve() {
  const root = path.resolve(dist);
  const rules = headerRules();
  const server = http.createServer((request, response) => {
    const urlPath = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
    const headers = {};
    for (const rule of rules) {
      if (!matches(rule.pattern, urlPath)) continue;
      for (const [name, value] of rule.headers) headers[name] = headers[name] ? `${headers[name]}, ${value}` : value;
    }
    if (dev && headers['Content-Security-Policy']) {
      headers['Content-Security-Policy'] = headers['Content-Security-Policy']
        .replace("script-src 'self'", "script-src 'self' 'unsafe-eval'");
    }
    const file = path.join(root, urlPath.endsWith('/') ? `${urlPath}index.html` : urlPath);
    if (!file.startsWith(root) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
      response.writeHead(404, { ...headers, 'Content-Type': 'text/html' });
      response.end(fs.readFileSync(path.join(dist, '404.html')));
      return;
    }
    headers['Content-Type'] = headers['Content-Type'] || TYPES[path.extname(file)] || 'application/octet-stream';
    headers['Cache-Control'] = headers['Cache-Control'] || 'public, max-age=0, must-revalidate';
    response.writeHead(200, headers);
    response.end(fs.readFileSync(file));
  });
  return new Promise((resolve) => server.listen(0, '127.0.0.1', () => resolve(server)));
}

const failures = [];
function check(name, ok, detail) {
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${name}${detail ? ` (${detail})` : ''}`);
  if (!ok) failures.push(name);
}

function visible(page, id) {
  return page.evaluate((elementId) => !document.getElementById(elementId).hidden, id);
}

async function openPage(browser, base, setup) {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
  const page = await context.newPage();
  const problems = [];
  page.on('pageerror', (error) => problems.push(`page error: ${error.message}`));
  page.on('console', (message) => {
    if (message.type() === 'error') problems.push(`console: ${message.text().slice(0, 200)}`);
  });
  await page.addInitScript(() => {
    document.addEventListener('securitypolicyviolation', (event) => {
      console.error(`CSP violation: ${event.violatedDirective} ${event.blockedURI}`);
    });
  });
  if (setup) await setup(page);
  await page.goto(base);
  return { context, page, problems };
}

(async () => {
  const server = await serve();
  const base = `http://127.0.0.1:${server.address().port}/`;
  const browser = await chromium.launch();
  try {
    const { context, page, problems } = await openPage(browser, base);
    check('loading screen shows first', await visible(page, 'boot'));
    await page.waitForFunction(() => document.getElementById('boot').hidden, null, { timeout: 60000 });
    check('app replaces the loading screen', !(await visible(page, 'failed')));
    await page.waitForFunction(async () => (await navigator.serviceWorker.ready).active.state === 'activated',
      null, { timeout: 120000 });
    const names = await page.evaluate(() => caches.keys());
    const cached = names.length === 1
      ? await page.evaluate(async (name) => (await (await caches.open(name)).keys()).length, names[0]) : 0;
    check('service worker caches the build', cached > 0, `${names.join(', ')}: ${cached} files`);
    await context.setOffline(true);
    await page.reload();
    await page.waitForFunction(() => document.getElementById('boot').hidden, null, { timeout: 60000 });
    check('reload works offline', !(await visible(page, 'failed')));
    check('no CSP violations or page errors', problems.length === 0, problems.join(' | '));
    await context.close();

    const tooOld = await openPage(browser, base, (p) => p.addInitScript(() => { WebAssembly.validate = () => false; }));
    await tooOld.page.waitForTimeout(1000);
    check('"too old" screen without WasmGC', await visible(tooOld.page, 'unsupported'));
    await tooOld.context.close();

    const broken = await openPage(browser, base, (p) => p.route('**/ember.js', (r) => r.fulfill({ status: 500 })));
    await broken.page.waitForTimeout(2000);
    check('"could not start" screen when the download fails', await visible(broken.page, 'failed'));
    await broken.context.close();
  } finally {
    await browser.close();
    server.close();
  }
  if (failures.length) {
    console.error(`${failures.length} check(s) failed.`);
    process.exit(1);
  }
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
