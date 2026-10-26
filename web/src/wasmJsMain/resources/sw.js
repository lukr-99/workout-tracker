// Offline copy of the browser edition.
//
// tools/stamp_web_dist.py fills in BUILD and PRECACHE after the Gradle build: every file of the
// distribution with its SHA-256. Each build gets its own cache, so a phone only ever runs one
// complete, matching set of files (page, ember.js, the .wasm files and
// the fonts). The browser re-checks this file on every visit (it is served uncached); when a deploy
// changes BUILD, the new set downloads in the background, is verified, and then replaces the old
// one. The old copy stays until the page that runs it hands over (SKIP_WAITING, sent by boot.js),
// so a running app never meets files of a newer build. Until this file is stamped (a local Gradle build) it caches nothing and stays out of the way.
'use strict';

const BUILD = '__EMBER_BUILD__';
const PRECACHE = /* __EMBER_PRECACHE__ */ [];

const PREFIX = 'ember-';
const CACHE = PREFIX + BUILD;
const STAMPED = !BUILD.startsWith('__');
const SCOPE_PATH = new URL('./', self.location).pathname;

function hex(buffer) {
  return Array.from(new Uint8Array(buffer), (b) => b.toString(16).padStart(2, '0')).join('');
}

// Fetch past the HTTP cache and check the bytes against the stamp, so a deploy that lands while a
// phone is mid-download cannot leave it with a mix of two builds. A mismatch fails the install;
// the old copy keeps working and the next visit tries again.
async function fetchVerified(path, sha256) {
  const response = await fetch(new Request(path, { cache: 'no-cache' }));
  if (!response.ok || response.redirected) throw new Error(`${path}: HTTP ${response.status}`);
  if (sha256 !== null) {
    const digest = hex(await crypto.subtle.digest('SHA-256', await response.clone().arrayBuffer()));
    if (digest !== sha256) throw new Error(`${path}: changed during download`);
  }
  return response;
}

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    if (STAMPED) {
      const cache = await caches.open(CACHE);
      try {
        await Promise.all(PRECACHE.map(async ([path, sha256]) => {
          await cache.put(path, await fetchVerified(path, sha256));
        }));
      } catch (error) {
        await caches.delete(CACHE);
        throw error;
      }
    }
    // No skipWaiting here: the new set waits until boot.js says the page may switch to it.
  })());
});

// Sent by boot.js when no app is running yet, or when the person pressed Reload on the update notice.
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    for (const name of await caches.keys()) {
      if (name.startsWith(PREFIX) && name !== CACHE) await caches.delete(name);
    }
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (!STAMPED || request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  // The page itself is cached as "./" (Cloudflare serves index.html at the directory URL).
  const isPage = request.mode === 'navigate' &&
    (url.pathname === SCOPE_PATH || url.pathname === SCOPE_PATH + 'index.html');
  event.respondWith((async () => {
    const cache = await caches.open(CACHE);
    const cached = isPage ? await cache.match('./') : await cache.match(request);
    return cached || fetch(request);
  })());
});
