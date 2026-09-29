package com.example.media

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/** On-device narration via the system TextToSpeech engine. */
class Narrator(private val context: Context) {
    private var tts: TextToSpeech? = null

    /** Returns true when an engine is ready and has a voice for [language]. */
    suspend fun prepare(language: String, speed: Float, pitch: Float): Boolean {
        shutdown()
        val ready = suspendCancellableCoroutine<Boolean> { cont ->
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context.applicationContext) { status ->
                if (cont.isActive) cont.resume(status == TextToSpeech.SUCCESS)
            }
            tts = engine
        }
        val engine = tts
        if (!ready || engine == null) return false
        val r = engine.setLanguage(localeFor(language))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        engine.setSpeechRate(speed.coerceIn(0.5f, 2f))
        engine.setPitch(pitch.coerceIn(0.5f, 2f))
        return true
    }

    /** Synthesizes [text] into a WAV file and returns the decoded audio, or null on failure. */
    suspend fun synthesize(text: String, id: String): WavData? {
        val engine = tts ?: return null
        val file = File(context.cacheDir, "tts_$id.wav")
        val ok = suspendCancellableCoroutine<Boolean> { cont ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { if (utteranceId == id && cont.isActive) cont.resume(true) }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { if (utteranceId == id && cont.isActive) cont.resume(false) }
                override fun onError(utteranceId: String?, errorCode: Int) { if (utteranceId == id && cont.isActive) cont.resume(false) }
            })
            val rc = engine.synthesizeToFile(text, null, file, id)
            if (rc != TextToSpeech.SUCCESS && cont.isActive) cont.resume(false)
        }
        val wav = if (ok && file.exists()) WavData.parse(file.readBytes()) else null
        file.delete()
        return wav
    }

    fun shutdown() {
        tts?.stop(); tts?.shutdown(); tts = null
    }

    companion object {
        fun localeFor(language: String): Locale = when (language.lowercase()) {
            "french" -> Locale.FRENCH
            "spanish" -> Locale("es")
            "german" -> Locale.GERMAN
            "portuguese" -> Locale("pt")
            "arabic" -> Locale("ar")
            "twi" -> Locale("ak")
            else -> Locale.US
        }
    }
}
