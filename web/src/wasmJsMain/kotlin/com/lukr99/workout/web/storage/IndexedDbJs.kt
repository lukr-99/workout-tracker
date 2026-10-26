package com.lukr99.workout.web.storage

import kotlin.js.Promise

// The browser calls behind the web edition's storage, each a single JavaScript expression. None of
// the promises rejects: a failure resolves to an object with `state: 'failed'` (or `ok: false`) and
// a readable `error`, because a rejected promise reaches Kotlin/Wasm without its message.

/**
 * Opens the IndexedDB database [name] at [version], creating the `data`, `photos` and `quarantine`
 * stores when they are missing. Resolves to `{ ok: true, db }` or `{ ok: false, error }`; it gives
 * up after [timeoutMs], since some browsers leave the request hanging in private mode.
 */
internal fun idbOpen(name: String, version: Int, timeoutMs: Int): Promise<JsAny> = js(
    """new Promise(function (resolve) {
        var done = false;
        function finish(value) { if (!done) { done = true; resolve(value); } }
        function describe(e) { return e ? (e.name ? e.name + ': ' + e.message : String(e)) : 'unknown error'; }
        try {
            if (typeof indexedDB === 'undefined' || !indexedDB) { finish({ ok: false, error: 'IndexedDB is not available' }); return; }
            var request = indexedDB.open(name, version);
            request.onupgradeneeded = function () {
                var db = request.result;
                ['data', 'photos', 'quarantine'].forEach(function (store) {
                    if (!db.objectStoreNames.contains(store)) db.createObjectStore(store);
                });
            };
            request.onsuccess = function () {
                var db = request.result;
                if (done) { db.close(); return; }
                db.onversionchange = function () { db.close(); };
                finish({ ok: true, db: db });
            };
            request.onerror = function () { finish({ ok: false, error: describe(request.error) }); };
            setTimeout(function () { finish({ ok: false, error: 'Opening the storage took too long' }); }, timeoutMs);
        } catch (e) {
            finish({ ok: false, error: describe(e) });
        }
    })""",
)

/** Reads the `current` record. Resolves to `{ state: 'empty' }`, `{ state: 'found', text, revision }` or a failure. */
internal fun idbLoad(db: JsAny): Promise<JsAny> = js(
    """new Promise(function (resolve) {
        function describe(e) { return e ? (e.name ? e.name + ': ' + e.message : String(e)) : 'unknown error'; }
        try {
            var request = db.transaction('data', 'readonly').objectStore('data').get('current');
            request.onsuccess = function () {
                var record = request.result;
                if (record === undefined || record === null) { resolve({ state: 'empty' }); return; }
                var text = typeof record.text === 'string' ? record.text
                    : (function () { try { return JSON.stringify(record); } catch (e) { return String(record); } })();
                resolve({ state: 'found', text: text, revision: Number(record.revision) || 0 });
            };
            request.onerror = function () { resolve({ state: 'failed', error: describe(request.error) }); };
        } catch (e) {
            resolve({ state: 'failed', error: describe(e) });
        }
    })""",
)

/**
 * In one `readwrite` transaction: reads the stored revision and, only when it is [expected],
 * writes [text] as revision [expected] + 1. Resolves after the transaction completes, to
 * `{ state: 'saved', revision }`, `{ state: 'conflict', revision }` or a failure (quota, blocked).
 */
internal fun idbSave(db: JsAny, text: String, expected: Double): Promise<JsAny> = js(
    """new Promise(function (resolve) {
        var done = false;
        function finish(value) { if (!done) { done = true; resolve(value); } }
        function describe(e) { return e ? (e.name ? e.name + ': ' + e.message : String(e)) : 'unknown error'; }
        try {
            var tx = db.transaction('data', 'readwrite');
            var store = tx.objectStore('data');
            var outcome = null;
            var read = store.get('current');
            read.onsuccess = function () {
                var stored = read.result ? (Number(read.result.revision) || 0) : 0;
                if (stored !== expected) { outcome = { state: 'conflict', revision: stored }; return; }
                var next = expected + 1;
                store.put({ revision: next, format: 1, text: text, savedAtUtc: Date.now() }, 'current');
                outcome = { state: 'saved', revision: next };
            };
            tx.oncomplete = function () { finish(outcome || { state: 'failed', error: 'The save did not finish' }); };
            tx.onerror = function () { finish({ state: 'failed', error: describe(tx.error) }); };
            tx.onabort = function () { finish({ state: 'failed', error: describe(tx.error) }); };
        } catch (e) {
            finish({ state: 'failed', error: describe(e) });
        }
    })""",
)

/**
 * Adds an entry to the `quarantine` store under [key]. Never overwrites: resolves to
 * `{ state: 'added' }`, `{ state: 'exists' }` when the key is taken, or a failure.
 */
internal fun idbQuarantine(db: JsAny, key: String, text: String, reason: String, atUtc: Double): Promise<JsAny> = js(
    """new Promise(function (resolve) {
        var done = false;
        function finish(value) { if (!done) { done = true; resolve(value); } }
        function describe(e) { return e ? (e.name ? e.name + ': ' + e.message : String(e)) : 'unknown error'; }
        try {
            var tx = db.transaction('quarantine', 'readwrite');
            var request = tx.objectStore('quarantine').add({ key: key, text: text, reason: reason, atUtc: atUtc }, key);
            request.onerror = function (event) {
                if (request.error && request.error.name === 'ConstraintError') {
                    event.preventDefault();
                    finish({ state: 'exists' });
                }
            };
            tx.oncomplete = function () { finish({ state: 'added' }); };
            tx.onabort = function () { finish({ state: 'failed', error: describe(tx.error) }); };
        } catch (e) {
            finish({ state: 'failed', error: describe(e) });
        }
    })""",
)

/** Every quarantine entry, oldest first. Resolves to `{ state: 'found', items }` or a failure. */
internal fun idbQuarantined(db: JsAny): Promise<JsAny> = js(
    """new Promise(function (resolve) {
        function describe(e) { return e ? (e.name ? e.name + ': ' + e.message : String(e)) : 'unknown error'; }
        try {
            var request = db.transaction('quarantine', 'readonly').objectStore('quarantine').getAll();
            request.onsuccess = function () {
                var items = (request.result || []).slice().sort(function (a, b) { return (a.atUtc || 0) - (b.atUtc || 0); });
                resolve({ state: 'found', items: items });
            };
            request.onerror = function () { resolve({ state: 'failed', error: describe(request.error) }); };
        } catch (e) {
            resolve({ state: 'failed', error: describe(e) });
        }
    })""",
)

/** Asks the browser to keep this site's data under storage pressure. Resolves to whether it agreed. */
internal fun requestPersist(): Promise<JsBoolean> = js(
    """(function () {
        try {
            if (typeof navigator !== 'undefined' && navigator.storage && navigator.storage.persist) {
                return navigator.storage.persist().then(function (granted) { return !!granted; }, function () { return false; });
            }
        } catch (e) {}
        return Promise.resolve(false);
    })()""",
)

/** Tells other tabs on the `ember-data` channel that a save reached [revision]. */
internal fun postDataChanged(revision: Double): Unit = js(
    """(function () {
        try {
            if (typeof BroadcastChannel === 'undefined') return;
            var channel = globalThis.emberDataChannel || (globalThis.emberDataChannel = new BroadcastChannel('ember-data'));
            if (channel.unref) channel.unref();
            channel.postMessage({ revision: revision });
        } catch (e) {}
    })()""",
)

/** Calls [onChange] whenever another tab says it saved. A tab does not hear its own notices. */
internal fun onDataChanged(onChange: () -> Unit): Unit = js(
    """(function () {
        try {
            if (typeof BroadcastChannel === 'undefined') return;
            var channel = globalThis.emberDataChannel || (globalThis.emberDataChannel = new BroadcastChannel('ember-data'));
            if (channel.unref) channel.unref();
            channel.addEventListener('message', function () { onChange(); });
        } catch (e) {}
    })()""",
)

/** Calls [onVisible] whenever the page comes back into view, for browsers that missed a notice. */
internal fun onPageVisible(onVisible: () -> Unit): Unit = js(
    """(function () {
        if (typeof document === 'undefined') return;
        document.addEventListener('visibilitychange', function () {
            if (document.visibilityState === 'visible') onVisible();
        });
    })()""",
)

/** [field] of [value] as text, or an empty string when it is missing. */
internal fun textField(value: JsAny, field: String): String =
    js("(value[field] === undefined || value[field] === null) ? '' : String(value[field])")

/** [field] of [value] as a number, or 0 when it is missing. */
internal fun numberField(value: JsAny, field: String): Double = js("Number(value[field]) || 0")

/** [field] of [value] as it is. */
internal fun anyField(value: JsAny, field: String): JsAny? = js("value[field]")

internal fun arrayLength(array: JsAny): Int = js("array.length")

internal fun arrayItem(array: JsAny, index: Int): JsAny = js("array[index]")
