package com.lukr99.workout.web.platform

import kotlin.js.Promise

// Taking, choosing and shrinking an exercise photo, and handing images to the share sheet.

/** The longest side of a stored photo, in pixels. */
internal const val PHOTO_MAX_EDGE = 1280

/** JPEG quality of a stored photo. */
internal const val PHOTO_QUALITY = 0.8

/**
 * Opens a hidden `file input that accepts images`; with [capture] the phone's camera opens
 * (Safari offers Take Photo or Photo Library either way). The chosen image is turned upright,
 * scaled so its longest side is at most [maxEdge] pixels and written as a JPEG at [quality].
 *
 * Resolves to `ok:<Base64 JPEG>`, `cancel` when the chooser is dismissed, or `error:<reason>`.
 * Must run inside a tap or click.
 */
internal fun pickPhoto(capture: Boolean, maxEdge: Int, quality: Double): Promise<JsString> = js(
    """new Promise(function (resolve) {
        var old = window.emberPhotoInput;
        if (old && old.parentNode) old.parentNode.removeChild(old);
        var input = document.createElement('input');
        window.emberPhotoInput = input;
        input.type = 'file';
        input.accept = 'image/*';
        if (capture) input.setAttribute('capture', 'environment');
        input.style.display = 'none';
        document.body.appendChild(input);
        function done() { if (input.parentNode) input.parentNode.removeChild(input); }
        function fail(e) { resolve('error:' + (e && e.message ? e.message : 'the image could not be read')); }
        input.addEventListener('cancel', function () { done(); resolve('cancel'); });
        input.addEventListener('change', function () {
            var file = input.files && input.files[0];
            done();
            if (!file) { resolve('cancel'); return; }
            var decoded;
            try {
                decoded = createImageBitmap(file, { imageOrientation: 'from-image' });
            } catch (e) { fail(e); return; }
            decoded.then(function (bitmap) {
                var scale = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
                var width = Math.max(1, Math.round(bitmap.width * scale));
                var height = Math.max(1, Math.round(bitmap.height * scale));
                var canvas = document.createElement('canvas');
                canvas.width = width;
                canvas.height = height;
                var context = canvas.getContext('2d');
                context.fillStyle = '#ffffff';
                context.fillRect(0, 0, width, height);
                context.drawImage(bitmap, 0, 0, width, height);
                if (bitmap.close) bitmap.close();
                canvas.toBlob(function (blob) {
                    if (!blob) { fail(null); return; }
                    var reader = new FileReader();
                    reader.onload = function () {
                        var text = String(reader.result);
                        resolve('ok:' + text.substring(text.indexOf(',') + 1));
                    };
                    reader.onerror = function () { fail(reader.error); };
                    reader.readAsDataURL(blob);
                }, 'image/jpeg', quality);
            }, fail);
        });
        input.click();
    })""",
)

/** Offers [base64] (a PNG) as a download named [fileName]. */
internal fun downloadBase64(fileName: String, base64: String, mimeType: String): Unit = js(
    """(function () {
        var text = atob(base64), bytes = new Uint8Array(text.length);
        for (var i = 0; i < text.length; i++) bytes[i] = text.charCodeAt(i);
        var url = URL.createObjectURL(new Blob([bytes], { type: mimeType }));
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
 * Shows the share sheet with an image file made from [base64]. Resolves to "shared", "cancelled",
 * "unavailable" (the browser cannot share files) or "blocked" (it refused). The sheet opens right
 * away, so call this inside the tap.
 */
internal fun shareImageFile(fileName: String, base64: String, mimeType: String, title: String): Promise<JsString> = js(
    """(function () {
        try {
            if (typeof navigator.share !== 'function' || typeof navigator.canShare !== 'function' || typeof File !== 'function') {
                return Promise.resolve('unavailable');
            }
            var text = atob(base64), bytes = new Uint8Array(text.length);
            for (var i = 0; i < text.length; i++) bytes[i] = text.charCodeAt(i);
            var data = { files: [new File([bytes], fileName, { type: mimeType })], title: title };
            if (!navigator.canShare(data)) return Promise.resolve('unavailable');
            return navigator.share(data).then(
                function () { return 'shared'; },
                function (e) { return e && e.name === 'AbortError' ? 'cancelled' : 'blocked'; });
        } catch (e) { return Promise.resolve('blocked'); }
    })()""",
)
