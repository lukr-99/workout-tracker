package com.lukr99.workout.web.platform

import kotlin.js.Promise

// Picking, reading, downloading and sharing files from the page.

/**
 * Opens the browser's file chooser through a hidden `<input type=file>`. Resolves to the chosen
 * `File`, or to null when the chooser is dismissed (where the browser says so). Must run inside a
 * tap or click.
 */
internal fun pickFile(accept: String): Promise<JsAny?> = js(
    """new Promise(function (resolve) {
        var old = window.emberFileInput;
        if (old && old.parentNode) old.parentNode.removeChild(old);
        var input = document.createElement('input');
        window.emberFileInput = input;
        input.type = 'file';
        input.accept = accept;
        input.style.display = 'none';
        document.body.appendChild(input);
        function done() { if (input.parentNode) input.parentNode.removeChild(input); }
        input.addEventListener('cancel', function () { done(); resolve(null); });
        input.addEventListener('change', function () {
            var file = input.files && input.files[0];
            done();
            resolve(file || null);
        });
        input.click();
    })""",
)

/** The name of a `File` from [pickFile]. */
internal fun pickedFileName(file: JsAny): String = js("file.name")

/** Reads a `File` from [pickFile] as text. Rejects when the file cannot be read. */
internal fun readPickedFileText(file: JsAny): Promise<JsString> = js(
    """new Promise(function (resolve, reject) {
        var reader = new FileReader();
        reader.onload = function () { resolve(String(reader.result)); };
        reader.onerror = function () { reject(reader.error || new Error('unreadable')); };
        reader.readAsText(file);
    })""",
)

/** Offers [text] as a download named [fileName]. */
internal fun downloadText(fileName: String, text: String, mimeType: String): Unit = js(
    """(function () {
        var url = URL.createObjectURL(new Blob([text], { type: mimeType }));
        var link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(function () { URL.revokeObjectURL(url); }, 60000);
    })()""",
)

/**
 * Shows the share sheet for [text] as a file named [fileName]. Resolves to "shared", "cancelled"
 * (the person closed the sheet), "unavailable" (the browser cannot share files) or "blocked" (it
 * refused, for example because the tap that started this is too old).
 */
internal fun shareTextFile(fileName: String, text: String, mimeType: String, title: String): Promise<JsString> = js(
    """(function () {
        try {
            if (typeof navigator.share !== 'function' || typeof navigator.canShare !== 'function' || typeof File !== 'function') {
                return Promise.resolve('unavailable');
            }
            var data = { files: [new File([text], fileName, { type: mimeType })], title: title };
            if (!navigator.canShare(data)) return Promise.resolve('unavailable');
            return navigator.share(data).then(
                function () { return 'shared'; },
                function (e) { return e && e.name === 'AbortError' ? 'cancelled' : 'blocked'; });
        } catch (e) { return Promise.resolve('blocked'); }
    })()""",
)
