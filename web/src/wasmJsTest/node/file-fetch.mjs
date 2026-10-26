// Preloaded into the Node.js test run (see web/build.gradle.kts).
//
// The test bundle links Compose, and with it Skia's browser build (skiko.mjs). That build only knows
// how to load its .wasm as a web page does, through XMLHttpRequest, and refuses Node's file system.
// This presents Node as a page just far enough for that one request, answered from disk; the tests
// themselves never touch a window or the DOM.
import { readFile } from 'node:fs';
import { fileURLToPath } from 'node:url';

if (typeof globalThis.window === 'undefined') globalThis.window = globalThis;

if (typeof globalThis.XMLHttpRequest === 'undefined') {
  globalThis.XMLHttpRequest = class {
    open(method, url) { this.url = String(url); }
    send() {
      readFile(fileURLToPath(this.url), (error, data) => {
        if (error) { this.status = 404; if (this.onerror) this.onerror(error); return; }
        this.status = 200;
        this.response = data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength);
        if (this.onload) this.onload();
      });
    }
  };
}
