package com.example.media

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.data.model.GeneratedSceneEntity
import java.io.File
import java.nio.ByteBuffer

/** Encodes scenes into an H.264/AAC MP4 with MediaCodec + MediaMuxer. */
class VideoExporter(private val options: RenderOptions = RenderOptions()) {

    private class Sample(val data: ByteArray, val info: MediaCodec.BufferInfo)

    /** [audio] maps sceneOrder -> narration audio (null/missing = silence). Returns the written file. */
    fun export(
        scenes: List<GeneratedSceneEntity>,
        audio: Map<Int, WavData?>,
        out: File,
        onProgress: (Float) -> Unit
    ): File {
        require(scenes.isNotEmpty()) { "No scenes to render" }
        out.parentFile?.mkdirs()
        if (out.exists()) out.delete()

        val hasAudio = audio.values.any { it != null }
        val rate = audio.values.firstNotNullOfOrNull { it?.sampleRate } ?: 22050
        val audioSamples = if (hasAudio) encodeAudio(scenes, audio, rate) else null
        val audioFormat = audioSamples?.first
        val audioData = audioSamples?.second ?: emptyList()

        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val w = options.width
        val h = options.height
        val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, options.fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        var videoTrack = -1
        var audioTrack = -1
        var muxerStarted = false
        val info = MediaCodec.BufferInfo()

        fun drain(endOfStream: Boolean) {
            while (true) {
                val idx = codec.dequeueOutputBuffer(info, if (endOfStream) 10_000 else 0)
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        videoTrack = muxer.addTrack(codec.outputFormat)
                        if (audioFormat != null) audioTrack = muxer.addTrack(audioFormat)
                        muxer.start(); muxerStarted = true
                        audioData.forEach { s -> muxer.writeSampleData(audioTrack, ByteBuffer.wrap(s.data), s.info) }
                    }
                    idx >= 0 -> {
                        val buf = codec.getOutputBuffer(idx)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && muxerStarted) {
                            buf.position(info.offset); buf.limit(info.offset + info.size)
                            muxer.writeSampleData(videoTrack, buf, info)
                        }
                        codec.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }

        val totalSec = scenes.sumOf { it.durationSeconds }
        val totalFrames = totalSec * options.fps
        val renderer = SceneFrameRenderer(options)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val starts = scenes.runningFold(0) { acc, s -> acc + s.durationSeconds }
        try {
            var sceneIdx = 0
            for (frame in 0 until totalFrames) {
                val t = frame.toFloat() / options.fps
                while (sceneIdx < scenes.size - 1 && t >= starts[sceneIdx + 1]) sceneIdx++
                renderer.render(bitmap, scenes[sceneIdx], sceneIdx, scenes.size, t - starts[sceneIdx], frame.toFloat() / totalFrames)
                bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
                var inIdx = codec.dequeueInputBuffer(10_000)
                while (inIdx < 0) { drain(false); inIdx = codec.dequeueInputBuffer(10_000) }
                val image = codec.getInputImage(inIdx)!!
                argbToYuv(pixels, w, h, image)
                codec.queueInputBuffer(inIdx, 0, w * h * 3 / 2, frame * 1_000_000L / options.fps, 0)
                drain(false)
                if (frame % options.fps == 0) onProgress(frame.toFloat() / totalFrames)
            }
            var eos = codec.dequeueInputBuffer(10_000)
            while (eos < 0) { drain(false); eos = codec.dequeueInputBuffer(10_000) }
            codec.queueInputBuffer(eos, 0, 0, totalFrames * 1_000_000L / options.fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drain(true)
        } finally {
            bitmap.recycle()
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
            try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
            muxer.release()
        }
        onProgress(1f)
        return out
    }

    private fun argbToYuv(px: IntArray, w: Int, h: Int, image: android.media.Image) {
        val yP = image.planes[0]; val uP = image.planes[1]; val vP = image.planes[2]
        val yB = yP.buffer; val uB = uP.buffer; val vB = vP.buffer
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val c = px[row + x]
                val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
                yB.put(y * yP.rowStride + x * yP.pixelStride, (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(16, 235).toByte())
                if (y % 2 == 0 && x % 2 == 0) {
                    val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(16, 240).toByte()
                    val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(16, 240).toByte()
                    uB.put((y / 2) * uP.rowStride + (x / 2) * uP.pixelStride, u)
                    vB.put((y / 2) * vP.rowStride + (x / 2) * vP.pixelStride, v)
                }
            }
        }
    }

    /** Places every scene's narration at its start time (silence elsewhere) and encodes AAC-LC mono. */
    private fun encodeAudio(scenes: List<GeneratedSceneEntity>, audio: Map<Int, WavData?>, rate: Int): Pair<MediaFormat, List<Sample>> {
        val total = scenes.sumOf { it.durationSeconds } * rate
        val pcm = ShortArray(total)
        var offset = 0
        scenes.forEach { s ->
            audio[s.sceneOrder]?.toMono(rate)?.let { m ->
                val n = minOf(m.size, s.durationSeconds * rate, total - offset)
                if (n > 0) System.arraycopy(m, 0, pcm, offset, n)
            }
            offset += s.durationSeconds * rate
        }
        val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val samples = ArrayList<Sample>()
        var outFormat: MediaFormat? = null
        val info = MediaCodec.BufferInfo()
        fun drain(eos: Boolean) {
            while (true) {
                val idx = codec.dequeueOutputBuffer(info, if (eos) 10_000 else 0)
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!eos) return
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outFormat = codec.outputFormat
                    idx >= 0 -> {
                        val buf = codec.getOutputBuffer(idx)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                            val bytes = ByteArray(info.size)
                            buf.position(info.offset); buf.get(bytes, 0, info.size)
                            val copy = MediaCodec.BufferInfo().also { it.set(0, info.size, info.presentationTimeUs, info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv()) }
                            samples.add(Sample(bytes, copy))
                        }
                        codec.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }
        try {
            var pos = 0
            val chunk = 2048 // samples per input buffer
            while (pos < pcm.size) {
                var idx = codec.dequeueInputBuffer(10_000)
                while (idx < 0) { drain(false); idx = codec.dequeueInputBuffer(10_000) }
                val buf = codec.getInputBuffer(idx)!!
                buf.clear()
                val n = minOf(chunk, pcm.size - pos, buf.capacity() / 2)
                for (i in 0 until n) buf.putShort(pcm[pos + i])
                codec.queueInputBuffer(idx, 0, n * 2, pos * 1_000_000L / rate, 0)
                pos += n
                drain(false)
            }
            var idx = codec.dequeueInputBuffer(10_000)
            while (idx < 0) { drain(false); idx = codec.dequeueInputBuffer(10_000) }
            codec.queueInputBuffer(idx, 0, 0, pos * 1_000_000L / rate, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drain(true)
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
        }
        return (outFormat ?: fmt) to samples
    }
}
