// Starts the browser edition: checks that this browser can run Kotlin/Wasm, loads ember.js,
// shows a loading screen until Compose draws its first frame, and keeps the offline copy current.
// Plain ES5-safe script on purpose, so even a browser far too old for the app can read it and show
// the "too old" screen. See docs/web-edition.md.
(function () {
  'use strict';

  // Minimal modules from the wasm-feature-detect project. The first declares a GC struct type
  // (WasmGC); the second uses try/catch_all (the legacy exception-handling proposal, which the
  // Kotlin 2.0 wasmJs target emits by default). Safari supports both from 18.2.
  var WASM_GC = new Uint8Array([0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 95, 1, 120, 0]);
  var WASM_EH = new Uint8Array([
    0, 97, 115, 109, 1, 0, 0, 0, 1, 4, 1, 96, 0, 0, 3, 2, 1, 0, 10, 8, 1, 6, 0, 6, 64, 25, 11, 11
  ]);
  var SLOW_AFTER_MS = 12000;

  var started = false;
  var failed = false;

  function byId(id) { return document.getElementById(id); }

  function canRunKotlinWasm() {
    try {
      return typeof WebAssembly === 'object' &&
        typeof WebAssembly.instantiateStreaming === 'function' &&
        WebAssembly.validate(WASM_GC) &&
        WebAssembly.validate(WASM_EH);
    } catch (e) {
      return false;
    }
  }

  function show(id) {
    var ids = ['boot', 'unsupported', 'failed'];
    for (var i = 0; i < ids.length; i++) byId(ids[i]).hidden = ids[i] !== id;
  }

  function fail(detail) {
    if (started || failed) return;
    failed = true;
    byId('failed-detail').textContent = detail ? String(detail).slice(0, 300) : '';
    show('failed');
  }

  // Called once Compose has drawn; also available to Kotlin as emberBoot.ready(). An early
  // error that did not stop the app (the canvas drew anyway) must not hide it behind "failed".
  function ready() {
    if (started) return;
    started = true;
    byId('failed').hidden = true;
    var boot = byId('boot');
    boot.classList.add('leaving');
    setTimeout(function () { boot.hidden = true; }, 320);
  }

  // Compose sets the canvas size when it attaches; two frames later the first frame is on screen.
  function watchCanvas() {
    var canvas = byId('ember');
    var observer = new MutationObserver(function () {
      observer.disconnect();
      requestAnimationFrame(function () { requestAnimationFrame(ready); });
    });
    observer.observe(canvas, { attributes: true, attributeFilter: ['width', 'height'] });
  }

  function loadApp() {
    watchCanvas();
    window.addEventListener('error', function (event) {
      fail(event.message || (event.error && event.error.message));
    });
    window.addEventListener('unhandledrejection', function (event) {
      var reason = event.reason;
      fail(reason && reason.message ? reason.message : reason);
    });
    setTimeout(function () { if (!started && !failed) byId('boot-slow').hidden = false; }, SLOW_AFTER_MS);

    var script = document.createElement('script');
    script.src = 'ember.js';
    script.onerror = function () { fail('Could not download ember.js.'); };
    document.head.appendChild(script);
  }

  // Safe-area insets (notch, home indicator) in CSS pixels. The canvas fills the whole window,
  // so the Compose UI reads these to keep controls clear of them.
  function safeArea() {
    var style = getComputedStyle(byId('safe-area-probe'));
    return {
      top: parseFloat(style.paddingTop) || 0,
      right: parseFloat(style.paddingRight) || 0,
      bottom: parseFloat(style.paddingBottom) || 0,
      left: parseFloat(style.paddingLeft) || 0
    };
  }

  // Safari still pinch-zooms the page despite user-scalable=no; the canvas has its own gestures.
  // (Double-tap zoom is off through touch-action: none in boot.css.)
  function blockPageZoom() {
    document.addEventListener('gesturestart', function (event) { event.preventDefault(); });
  }

  // Offline copy. A new deploy installs in the background and waits (sw.js does not skip the wait),
  // so a running app never meets the files of a newer build. A page still on the loading screen hands
  // over at once; a running app gets the update notice, and its Reload button does the handover.
  var registration = null;
  var handingOver = false;

  function handOver(worker) {
    handingOver = true;
    worker.postMessage({ type: 'SKIP_WAITING' });
  }

  function registerServiceWorker() {
    if (!('serviceWorker' in navigator)) return;
    var hadController = !!navigator.serviceWorker.controller;
    navigator.serviceWorker.addEventListener('controllerchange', function () {
      if (!hadController) { hadController = true; return; }
      if (!started || handingOver) { location.reload(); return; }
      byId('update').hidden = false;
    });
    // A new build is ready and waiting. With no worker in charge yet this is the first install.
    function offer(worker) {
      if (!navigator.serviceWorker.controller) return;
      if (!started) handOver(worker); else byId('update').hidden = false;
    }
    navigator.serviceWorker.register('sw.js').then(function (reg) {
      registration = reg;
      if (reg.waiting) offer(reg.waiting);
      reg.addEventListener('updatefound', function () {
        var worker = reg.installing;
        if (!worker) return;
        worker.addEventListener('statechange', function () {
          if (worker.state === 'installed') offer(worker);
        });
      });
      // Home-screen apps on iOS are often resumed rather than relaunched; check for updates then.
      document.addEventListener('visibilitychange', function () {
        if (document.visibilityState === 'visible') reg.update().catch(function () {});
      });
    }).catch(function () { /* The app still works online without the offline copy. */ });
  }

  // The Reload button: switch to the waiting build first if there is one, then reload.
  function reloadForUpdate() {
    if (registration && registration.waiting) handOver(registration.waiting);
    else location.reload();
  }

  // Your workouts live in this browser's storage. Ask for it to be kept when the device runs low on
  // space; browsers that support this usually grant it to installed (home-screen) apps.
  function askToKeepStorage() {
    if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(function () {});
  }

  window.emberBoot = { ready: ready };
  window.emberSafeArea = safeArea;
  window.addEventListener('resize', function () {
    window.dispatchEvent(new CustomEvent('ember:safearea', { detail: safeArea() }));
  });

  document.addEventListener('click', function (event) {
    var target = event.target;
    if (target && target.getAttribute && target.getAttribute('data-action') === 'reload') reloadForUpdate();
  });

  blockPageZoom();
  if (!canRunKotlinWasm()) {
    show('unsupported');
    return;
  }
  registerServiceWorker();
  askToKeepStorage();
  loadApp();
})();
