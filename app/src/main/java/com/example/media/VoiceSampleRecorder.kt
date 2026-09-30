package com.example.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Records the microphone (16 kHz mono) until cancelled or [maxSeconds] is reached, returning WAV bytes. */
object VoiceSampleRecorder {
    const val RATE = 16_000

    @SuppressLint("MissingPermission")
    suspend fun record(maxSeconds: Int, shouldStop: () -> Boolean, onLevel: (Float) -> Unit = {}): ByteArray = withContext(Dispatchers.IO) {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, RATE))
        check(rec.state == AudioRecord.STATE_INITIALIZED) { "The microphone could not be opened" }
        val all = ShortArray(RATE * maxSeconds)
        var n = 0
        try {
            rec.startRecording()
            val buf = ShortArray(1600)
            while (!shouldStop() && n < all.size) {
                val r = rec.read(buf, 0, minOf(buf.size, all.size - n))
                if (r <= 0) break
                System.arraycopy(buf, 0, all, n, r); n += r
                var peak = 0
                for (i in 0 until r) peak = maxOf(peak, Math.abs(buf[i].toInt()))
                onLevel(peak / 32768f)
            }
        } finally {
            try { rec.stop() } catch (_: Exception) {}
            rec.release()
        }
        WavWriter.write(all.copyOf(n), RATE)
    }
}
