package com.lukr99.workout.data.run

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.lukr99.workout.domain.run.SplitCue
import java.util.Locale

/**
 * Thin, isolated Android side-effect layer for the live-run cues — the only place Run Mode touches
 * TextToSpeech or the vibrator. The pure "did a split just cross" decision lives in [SplitCue]; this
 * just turns a crossed mark into a spoken split + a short buzz, plays a tick on each countdown number,
 * and a finish flourish. Deliberately fire-and-forget and null-safe so a missing TTS engine or
 * vibrator silently degrades (the run is never affected).
 *
 * Own one per live-run screen and [release] it when the screen leaves. [hapticsEnabled]/[voiceEnabled]
 * gate the two channels independently; [release] is idempotent.
 */
class RunCues(
    context: Context,
    var hapticsEnabled: Boolean = true,
    var voiceEnabled: Boolean = true,
) {
    private val appContext = context.applicationContext
    private var ttsReady = false

    private val tts: TextToSpeech? = runCatching {
        TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                runCatching { tts?.language = Locale.getDefault() }
            }
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    /** Announce crossing split [index] (unit-aware) — a double buzz + spoken distance/pace. */
    fun splitCue(index: Int, imperial: Boolean, avgPaceSecPerUnit: Double) {
        buzz(longArrayOf(0, 120, 90, 120))
        speak(SplitCue.spokenMessage(index, imperial, avgPaceSecPerUnit))
    }

    /** A short tick for each countdown number (3-2-1). */
    fun countdownTick() = buzz(longArrayOf(0, 60))

    /** A longer flourish when the run starts recording. */
    fun startCue() = buzz(longArrayOf(0, 200))

    /** Spoken + haptic finish summary flourish. */
    fun finishCue(distanceText: String, durationText: String) {
        buzz(longArrayOf(0, 200, 120, 200, 120, 300))
        speak("Run complete. $distanceText in $durationText.")
    }

    private fun speak(text: String) {
        if (!voiceEnabled || !ttsReady) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "run-cue") }
    }

    @Suppress("DEPRECATION")
    private fun buzz(pattern: LongArray) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                v.vibrate(pattern, -1)
            }
        }
    }

    /** Stop + free the TTS engine. Safe to call more than once. */
    fun release() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
    }
}
