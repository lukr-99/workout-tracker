package com.lukr99.workout.web.storage

import kotlin.js.Promise

// The browser calls behind the photo store, in the same style as IndexedDbJs.kt: none of the
// promises rejects, and photo bytes cross as Base64 text. A record in the `photos` store is
// `{ bytes: ArrayBuffer, savedAtUtc }` under the photo's key.

/** Every photo. Resolves to `{ state: 'found', keys: string[], data: string[] }` (Base64) or a failure. */
internal fun idbPhotosLoadAll(db: JsAny): Promise<JsAny> = js(
    """new Promise(function (resolve) {
        function describe(e) { return e ? (e.name ? e.name + ': ' + e.message : String(e)) : 'unknown error'; }
        function toBase64(buffer) {
            var bytes = new Uint8Array(buffer), text = '';
            for (var i = 0; i < bytes.length; i += 0x8000) {
                text += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
            }
            return btoa(text);
        }
        try {
            var store = db.transaction('photos', 'readonly').objectStore('photos');
            var keys = store.getAllKeys();
            var values = store.getAll();
            var tx = store.transaction;
            tx.oncomplete = function () {
                var outKeys = [], outData = [];
                (keys.result || []).forEach(function (key, index) {
                    var record = values.result[index];
                    if (record && record.bytes) { outKeys.push(String(key)); outData.push(toBase64(record.bytes)); }
                });
                resolve({ state: 'found', keys: outKeys, data: outData });
            };
            tx.onerror = function () { resolve({ state: 'failed', error: describe(tx.error) }); };
            tx.onabort = function () { resolve({ state: 'failed', error: describe(tx.error) }); };
        } catch (e) {
            resolve({ state: 'failed', error: describe(e) });
        }
    })""",
)

/**
 * Writes one photo. Resolves after the transaction completes, to `{ state: 'saved' }` or
 * `{ state: 'failed', error, full }` where `full` says the device ran out of space.
 */
internal fun idbPhotoPut(db: JsAny, key: String, base64: String): Promise<JsAny> = js(
    """new Promise(function (resolve) {
        var done = false;
        function finish(value) { if (!done) { done = true; resolve(value); } }
        function describe(e) { return e ? (e.name ? e.name + ': ' + e.message : String(e)) : 'unknown error'; }
        function failure(e) { finish({ state: 'failed', error: describe(e), full: !!(e && e.name === 'QuotaExceededError') }); }
        try {
            var text = atob(base64), bytes = new Uint8Array(text.length);
            for (var i = 0; i < text.length; i++) bytes[i] = text.charCodeAt(i);
            var tx = db.transaction('photos', 'readwrite');
            tx.objectStore('photos').put({ bytes: bytes.buffer, savedAtUtc: Date.now() }, key);
            tx.oncomplete = function () { finish({ state: 'saved' }); };
            tx.onerror = function () { failure(tx.error); };
            tx.onabort = function () { failure(tx.error); };
        } catch (e) {
            failure(e);
        }
    })""",
)

/** Removes the photos whose keys are listed one per line in [keys]. Resolves to `{ state: 'deleted' }` or a failure. */
internal fun idbPhotosDelete(db: JsAny, keys: String): Promise<JsAny> = js(
    """new Promise(function (resolve) {
        var done = false;
        function finish(value) { if (!done) { done = true; resolve(value); } }
        function describe(e) { return e ? (e.name ? e.name + ': ' + e.message : String(e)) : 'unknown error'; }
        try {
            var tx = db.transaction('photos', 'readwrite');
            var store = tx.objectStore('photos');
            keys.split('\n').forEach(function (key) { if (key) store.delete(key); });
            tx.oncomplete = function () { finish({ state: 'deleted' }); };
            tx.onerror = function () { finish({ state: 'failed', error: describe(tx.error) }); };
            tx.onabort = function () { finish({ state: 'failed', error: describe(tx.error) }); };
        } catch (e) {
            finish({ state: 'failed', error: describe(e) });
        }
    })""",
)

/** An array of text from [field] of [value]. */
internal fun textArrayField(value: JsAny, field: String): JsAny = js("value[field]")

/** The text at [index] of a JavaScript array of text. */
internal fun arrayText(array: JsAny, index: Int): String = js("String(array[index])")
