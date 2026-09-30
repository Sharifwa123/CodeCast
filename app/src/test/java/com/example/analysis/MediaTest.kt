package com.example.analysis

import com.example.data.model.GeneratedSceneEntity
import com.example.media.SrtWriter
import com.example.media.WavData
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MediaTest {
    private fun wav(rate: Int, ch: Int, samples: ShortArray): ByteArray {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply { samples.forEach { putShort(it) } }.array()
        val b = ByteBuffer.allocate(44 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + data.size).put("WAVE".toByteArray()).put("fmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(ch.toShort()).putInt(rate).putInt(rate * ch * 2).putShort((ch * 2).toShort()).putShort(16)
            .put("data".toByteArray()).putInt(data.size).put(data)
        return b.array()
    }

    @Test fun wavParsesLengthAndResamples() {
        val w = WavData.parse(wav(22050, 1, ShortArray(22050) { (it % 100).toShort() }))!!
        assertEquals(1.0, w.seconds, 0.001)
        assertEquals(44100.0, w.toMono(44100).size.toDouble(), 2.0)
        val stereo = WavData.parse(wav(8000, 2, ShortArray(16000) { 1000 }))!!
        assertEquals(1.0, stereo.seconds, 0.001)
        assertTrue(stereo.toMono(8000).all { it.toInt() == 1000 })
    }

    @Test fun wavWithHeaderCoveringOnlyFirstChunkStillReadsAllAudio() {
        val full = wav(22050, 1, ShortArray(44100) { 500 }) // 2s
        val b = ByteBuffer.wrap(full).order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(40, 2205 * 2) // header claims only 0.1s of data
        assertEquals(2.0, WavData.parse(full)!!.seconds, 0.001)
    }

    @Test fun wavRejectsGarbage() {
        assertNull(WavData.parse(ByteArray(10)))
        assertNull(WavData.parse(ByteArray(64)))
    }

    @Test fun srtTimingsAccumulate() {
        fun sc(o: Int, d: Int, t: String) = GeneratedSceneEntity(tutorialId = 1, sceneOrder = o, screenDrawableName = "", title = "t", narrationScript = t, subtitleText = t, durationSeconds = d)
        val srt = SrtWriter.build(listOf(sc(1, 5, "one"), sc(2, 65, "two")))
        assertTrue(srt.contains("1\n00:00:00,000 --> 00:00:05,000\none"))
        assertTrue(srt.contains("2\n00:00:05,000 --> 00:01:10,000\ntwo"))
    }
}
