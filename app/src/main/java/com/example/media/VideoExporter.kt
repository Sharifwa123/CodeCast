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

    /** Places every scene's narration at its start time (silence elsewhere) and encodes AAC-LC mono. */
    private fun encodeAudio(scenes: List<GeneratedSceneEntity>, audio: Map<Int, WavData?>, rate: Int): Pair<MediaFormat, List<AudioSample>> {
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
        return encodeAacPcm(pcm, rate)
    }
}
