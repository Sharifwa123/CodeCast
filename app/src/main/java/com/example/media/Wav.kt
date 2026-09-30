package com.example.media

/** 16-bit PCM audio decoded from a WAV file (as written by TextToSpeech.synthesizeToFile). */
class WavData(val sampleRate: Int, val channels: Int, val pcm: ShortArray) {
    val seconds: Double get() = pcm.size.toDouble() / channels / sampleRate

    /** Mono samples at [targetRate] (linear resample, channel downmix). */
    fun toMono(targetRate: Int): ShortArray {
        val mono = if (channels == 1) pcm else ShortArray(pcm.size / channels) { i ->
            var sum = 0
            for (c in 0 until channels) sum += pcm[i * channels + c]
            (sum / channels).toShort()
        }
        if (sampleRate == targetRate || mono.isEmpty()) return mono
        val outLen = (mono.size.toLong() * targetRate / sampleRate).toInt()
        return ShortArray(outLen) { i ->
            val pos = i.toDouble() * sampleRate / targetRate
            val i0 = pos.toInt().coerceAtMost(mono.size - 1)
            val i1 = (i0 + 1).coerceAtMost(mono.size - 1)
            val f = pos - i0
            (mono[i0] * (1 - f) + mono[i1] * f).toInt().toShort()
        }
    }

    companion object {
        fun parse(bytes: ByteArray): WavData? {
            if (bytes.size < 44 || String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return null
            var pos = 12
            var rate = 0
            var ch = 0
            var bits = 0
            while (pos + 8 <= bytes.size) {
                val id = String(bytes, pos, 4)
                var size = le32(bytes, pos + 4)
                val body = pos + 8
                if (id == "fmt ") {
                    ch = le16(bytes, body + 2); rate = le32(bytes, body + 4); bits = le16(bytes, body + 14)
                } else if (id == "data") {
                    if (bits != 16 || ch < 1 || rate <= 0) return null
                    // streaming writers may leave the size as 0 / 0xFFFFFFFF: use the rest of the file
                    val rest = bytes.size - body
                    if (size <= 0 || size > rest) size = rest
                    else if (size < rest) {
                        // Some engines write a header that only covers the first chunk. Trust the bytes present
                        // unless a known trailing chunk follows.
                        val next = if (body + size + 4 <= bytes.size) String(bytes, body + size, 4) else ""
                        if (next != "LIST" && next != "id3 " && next != "ID3 " && next != "fact") size = rest
                    }
                    val n = size / 2
                    val out = ShortArray(n) { i -> (le16(bytes, body + i * 2)).toShort() }
                    return WavData(rate, ch, out)
                }
                pos = body + size + (size and 1)
            }
            return null
        }

        private fun le16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
        private fun le32(b: ByteArray, o: Int) = le16(b, o) or (le16(b, o + 2) shl 16)
    }
}
