package com.example.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat

class AudioSample(val data: ByteArray, val info: MediaCodec.BufferInfo)

/** Encodes mono 16-bit PCM to AAC-LC access units. */
internal fun encodeAacPcm(pcm: ShortArray, rate: Int): Pair<MediaFormat, List<AudioSample>> {
        val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val samples = ArrayList<AudioSample>()
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
                            samples.add(AudioSample(bytes, copy))
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
