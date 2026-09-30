package com.example.media

import android.graphics.Bitmap
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

internal fun argbToYuv(px: IntArray, w: Int, h: Int, image: Image) {
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

/** Streams frames with explicit timestamps into a video-only H.264 MP4 (used for real-time recordings). */
class FrameEncoder(private val w: Int, private val h: Int, private val fps: Int, private val out: File) {
    private val codec: MediaCodec
    private val muxer: MediaMuxer
    private var track = -1
    private var started = false
    private val info = MediaCodec.BufferInfo()
    private val pixels = IntArray(w * h)
    private var lastPts = -1L

    init {
        out.parentFile?.mkdirs(); if (out.exists()) out.delete()
        muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, if (w * h > 1_000_000) 3_500_000 else 2_500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
    }

    private fun drain(eos: Boolean) {
        while (true) {
            val idx = codec.dequeueOutputBuffer(info, if (eos) 10_000 else 0)
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!eos) return
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track = muxer.addTrack(codec.outputFormat); muxer.start(); started = true }
                idx >= 0 -> {
                    val buf = codec.getOutputBuffer(idx)!!
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && started) { buf.position(info.offset); buf.limit(info.offset + info.size); muxer.writeSampleData(track, buf, info) }
                    codec.releaseOutputBuffer(idx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    /** [bitmap] must be exactly w x h. */
    fun addFrame(bitmap: Bitmap, ptsUs: Long) {
        val pts = if (ptsUs <= lastPts) lastPts + 1000 else ptsUs
        lastPts = pts
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var i = codec.dequeueInputBuffer(10_000)
        while (i < 0) { drain(false); i = codec.dequeueInputBuffer(10_000) }
        argbToYuv(pixels, w, h, codec.getInputImage(i)!!)
        codec.queueInputBuffer(i, 0, w * h * 3 / 2, pts, 0)
        drain(false)
    }

    fun finish() {
        try {
            var i = codec.dequeueInputBuffer(10_000)
            while (i < 0) { drain(false); i = codec.dequeueInputBuffer(10_000) }
            codec.queueInputBuffer(i, 0, 0, lastPts + 1000, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drain(true)
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
            try { if (started) muxer.stop() } catch (_: Exception) {}
            muxer.release()
        }
    }
}

object Mux {
    /** Copies the video track of [video] into [out] and adds the narration track. */
    fun combine(video: File, audio: Pair<MediaFormat, List<AudioSample>>?, out: File) {
        if (out.exists()) out.delete()
        val ex = MediaExtractor().apply { setDataSource(video.absolutePath) }
        var vIdx = -1
        for (t in 0 until ex.trackCount) if (ex.getTrackFormat(t).getString(MediaFormat.KEY_MIME)!!.startsWith("video/")) vIdx = t
        require(vIdx >= 0) { "Recording produced no video track" }
        ex.selectTrack(vIdx)
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val vTrack = muxer.addTrack(ex.getTrackFormat(vIdx))
            val aTrack = audio?.let { muxer.addTrack(it.first) } ?: -1
            muxer.start()
            audio?.second?.forEach { muxer.writeSampleData(aTrack, ByteBuffer.wrap(it.data), it.info) }
            val buf = ByteBuffer.allocate(2 * 1024 * 1024)
            val bi = MediaCodec.BufferInfo()
            while (true) {
                buf.clear()
                val n = ex.readSampleData(buf, 0)
                if (n < 0) break
                bi.set(0, n, ex.sampleTime, if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                muxer.writeSampleData(vTrack, buf, bi)
                ex.advance()
            }
        } finally {
            ex.release()
            try { muxer.stop() } catch (_: Exception) {}
            muxer.release()
        }
    }
}
