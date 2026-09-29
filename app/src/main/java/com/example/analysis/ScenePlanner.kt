package com.example.analysis

import com.example.data.model.*

/** Text translation hook (Gemini in the app). Returns null when translation is unavailable. */
interface Translator {
    suspend fun translate(texts: List<String>, targetLanguage: String): List<String>?
}

data class DurationRange(val minSec: Int, val maxSec: Int?)

object ScenePlanner {
    const val WORDS_PER_SECOND = 2.5
    const val MIN_SCENE_SECONDS = 3

    fun rangeFor(duration: String): DurationRange = when {
        duration.startsWith("Quick") -> DurationRange(30, 60)
        duration.startsWith("Detailed") -> DurationRange(180, 420)
        duration.startsWith("Full") -> DurationRange(420, null)
        else -> DurationRange(60, 180)
    }

    fun narrationFor(step: TutorialStepEntity, index: Int, total: Int): String {
        val screen = step.screenName.ifBlank { "the application" }
        val what = when (step.actionType) {
            "Navigate" -> "Open $screen. ${step.instruction.ifBlank { "" }}"
            "Input" -> "On $screen, ${step.instruction.ifBlank { step.title }}."
            "Click" -> "On $screen, ${step.instruction.ifBlank { step.title }}."
            "Request" -> "${step.title}. ${step.instruction}"
            else -> "${step.title}. ${step.instruction}"
        }.replace(Regex("""\s+"""), " ").trim().replace(Regex("""\.+$"""), ".")
        val intro = if (index == 0) "In this tutorial we start here. " else ""
        val outro = if (index == total - 1) " That completes this walkthrough." else ""
        return (intro + what + outro).trim()
    }

    fun words(text: String) = text.trim().split(Regex("""\s+""")).count { it.isNotEmpty() }

    /** Natural spoken duration for [text] at [speed], in seconds. */
    fun spokenSeconds(text: String, speed: Float): Double = words(text) / (WORDS_PER_SECOND * speed.coerceIn(0.5f, 2f))

    /** Builds scenes for the checked steps. [speechSeconds] (measured TTS audio length) wins over the estimate. */
    fun plan(
        tutorialId: Int,
        steps: List<TutorialStepEntity>,
        tutorial: TutorialPlanEntity,
        speechSeconds: (String) -> Double? = { null }
    ): List<GeneratedSceneEntity> {
        val active = steps.filter { it.isChecked }
        if (active.isEmpty()) return emptyList()
        val narrations = active.mapIndexed { i, s -> narrationFor(s, i, active.size) }
        var seconds = narrations.map { n ->
            val natural = speechSeconds(n) ?: spokenSeconds(n, tutorial.speakingSpeed)
            (natural + 1.0).coerceAtLeast(MIN_SCENE_SECONDS.toDouble())
        }
        val range = rangeFor(tutorial.duration)
        val total = seconds.sum()
        // Fit into the requested range without cutting speech: only pad up, or shrink pauses down to speech length.
        if (total < range.minSec) {
            val extra = (range.minSec - total) / seconds.size
            seconds = seconds.map { it + extra }
        }
        return active.mapIndexed { i, s ->
            val hl = s.codeFilePath.isNotEmpty()
            GeneratedSceneEntity(
                tutorialId = tutorialId,
                sceneOrder = i + 1,
                screenDrawableName = "",
                title = s.title,
                narrationScript = narrations[i],
                subtitleText = if (tutorial.hasSubtitles) narrations[i] else "",
                durationSeconds = Math.ceil(seconds[i]).toInt(),
                zoomTarget = if (s.actionType == "Input" || s.actionType == "Click") "Action Control" else "Center",
                calloutText = "${s.actionType}: ${s.title}",
                transitionType = tutorial.transitionStyle,
                codeFilePath = s.codeFilePath,
                codeSnippet = s.codeSnippet,
                highlightedLines = if (hl) s.evidenceSource.let { if (s.codeSnippet.isNotEmpty()) hlFrom(s) else "1" } else "1",
                terminalOutput = if (s.evidenceSource.isNotEmpty()) "evidence: ${s.evidenceSource}  [${s.verificationStatus}]" else ""
            )
        }
    }

    private fun hlFrom(s: TutorialStepEntity): String {
        val line = s.evidenceSource.substringAfterLast(':', "").toIntOrNull() ?: return "1"
        // snippet starts up to 3 lines before the evidence line (see CodebaseAnalyzer.snippetAround)
        return (minOf(line, 4)).toString()
    }

    fun isSameLanguage(a: String, b: String) = a.isBlank() || b.isBlank() || a.equals(b, ignoreCase = true)

    /** Applies narration/subtitle translation. Returns scenes plus whether translation actually happened. */
    suspend fun localize(
        scenes: List<GeneratedSceneEntity>, tutorial: TutorialPlanEntity, translator: Translator?
    ): Pair<List<GeneratedSceneEntity>, LocalizationResult> {
        var narrationOk = true
        var subtitleOk = true
        var out = scenes
        if (!isSameLanguage(tutorial.narrationLang, "English")) {
            val t = translator?.translate(out.map { it.narrationScript }, tutorial.narrationLang)
            if (t != null && t.size == out.size) {
                out = out.mapIndexed { i, s -> s.copy(narrationScript = t[i], subtitleText = if (tutorial.hasSubtitles) t[i] else "") }
            } else narrationOk = false
        }
        if (tutorial.hasSubtitles && !isSameLanguage(tutorial.subtitleLang, tutorial.narrationLang)) {
            val t = translator?.translate(out.map { it.narrationScript }, tutorial.subtitleLang)
            if (t != null && t.size == out.size) out = out.mapIndexed { i, s -> s.copy(subtitleText = t[i]) } else subtitleOk = false
        }
        return out to LocalizationResult(narrationOk, subtitleOk)
    }
}

data class LocalizationResult(val narrationTranslated: Boolean, val subtitlesTranslated: Boolean)
