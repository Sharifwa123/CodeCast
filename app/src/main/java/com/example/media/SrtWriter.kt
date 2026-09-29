package com.example.media

import com.example.data.model.GeneratedSceneEntity

object SrtWriter {
    fun build(scenes: List<GeneratedSceneEntity>): String {
        val sb = StringBuilder()
        var t = 0
        var n = 1
        scenes.forEach { s ->
            val text = s.subtitleText.ifBlank { s.narrationScript }
            if (text.isNotBlank()) {
                sb.append(n++).append('\n').append(stamp(t)).append(" --> ").append(stamp(t + s.durationSeconds)).append('\n')
                    .append(text.trim()).append("\n\n")
            }
            t += s.durationSeconds
        }
        return sb.toString()
    }

    fun stamp(totalSeconds: Int): String = "%02d:%02d:%02d,000".format(totalSeconds / 3600, totalSeconds / 60 % 60, totalSeconds % 60)
}
