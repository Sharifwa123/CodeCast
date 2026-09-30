package com.example.analysis

import com.example.data.model.*

/** Real checks over the actual steps, scenes and codebase. */
object QualityChecker {
    fun run(
        steps: List<TutorialStepEntity>,
        scenes: List<GeneratedSceneEntity>,
        tutorial: TutorialPlanEntity,
        files: List<CodebaseFile>,
        localization: LocalizationResult = LocalizationResult(true, true),
        audioAvailable: Boolean = true
    ): QualityCheckReport {
        val issues = mutableListOf<QualityCheckIssue>()
        val active = steps.filter { it.isChecked }
        var total = 0
        var passed = 0
        fun check(ok: Boolean) { total++; if (ok) passed++ }

        // 1. Steps exist and are backed by verified evidence
        val unverified = active.filter { it.verificationStatus == "UNABLE_TO_VERIFY" }
        unverified.forEach {
            issues.add(QualityCheckIssue(it.stepOrder, it.title, "Unverified Screen",
                "Step '${it.title}' could not be matched to source code.", "Remove the step or map it to a screen found in the code."))
        }
        if (active.isEmpty()) issues.add(QualityCheckIssue(0, "Tutorial", "No Steps", "No steps are selected, so there is nothing to show.", "Select at least one step."))
        check(active.isNotEmpty() && unverified.isEmpty())

        // 2. Evidence: each scene points at a real file in the codebase and the snippet is really in it
        val byPath = files.associateBy { it.path }
        var evidenceOk = true
        scenes.forEach { sc ->
            val f = byPath[sc.codeFilePath]
            val first = sc.codeSnippet.lines().firstOrNull { it.isNotBlank() }?.trim()
            if (f == null || sc.codeSnippet.isBlank() || (first != null && !f.content.contains(first))) {
                evidenceOk = false
                issues.add(QualityCheckIssue(sc.sceneOrder, sc.title, "Missing Evidence",
                    "Scene ${sc.sceneOrder} has no code evidence found in the project source.", "Re-select the step from a discovered workflow."))
            }
        }
        active.filter { it.verificationStatus == "INFERRED" }.forEach {
            issues.add(QualityCheckIssue(it.stepOrder, it.title, "Inferred UI Element",
                "'${it.title}' was inferred rather than matched to a concrete element.", "Verify in the evidence view."))
        }
        check(scenes.isNotEmpty() && evidenceOk && active.none { it.verificationStatus == "INFERRED" })

        // 3. Narration & subtitles present and consistent with steps
        var textOk = scenes.size == active.size
        if (!textOk) issues.add(QualityCheckIssue(0, "Tutorial", "Scene Mismatch", "${scenes.size} scenes for ${active.size} steps.", "Regenerate the tutorial."))
        scenes.forEach { sc ->
            if (sc.narrationScript.isBlank() || (tutorial.hasSubtitles && sc.subtitleText.isBlank())) {
                textOk = false
                issues.add(QualityCheckIssue(sc.sceneOrder, sc.title, "Empty Text", "Scene ${sc.sceneOrder} has empty narration or subtitle.", "Edit the scene text."))
            }
        }
        if (!localization.narrationTranslated) {
            textOk = false
            issues.add(QualityCheckIssue(0, "Narration", "Not Translated", "Narration could not be translated to ${tutorial.narrationLang}; it is in English.", "Add a Gemini API key on the Voice & Language step, or choose English."))
        }
        if (tutorial.hasSubtitles && !localization.subtitlesTranslated) {
            textOk = false
            issues.add(QualityCheckIssue(0, "Subtitles", "Not Translated", "Subtitles could not be translated to ${tutorial.subtitleLang}; they use the narration language.", "Add a Gemini API key on the Voice & Language step, or match the narration language."))
        }
        check(textOk)

        // 4. Timing: total inside requested range, each scene long enough to speak its narration
        val range = ScenePlanner.rangeFor(tutorial.duration)
        val sum = scenes.sumOf { it.durationSeconds }
        var timingOk = scenes.isNotEmpty() && (range.maxSec == null || sum <= range.maxSec)
        if (!timingOk) issues.add(QualityCheckIssue(0, "Tutorial", "Duration", "Total ${sum}s is longer than ${tutorial.duration}.", "Remove steps or pick a longer duration."))
        scenes.forEach { sc ->
            val need = ScenePlanner.spokenSeconds(sc.narrationScript, tutorial.speakingSpeed)
            if (sc.durationSeconds < need) {
                timingOk = false
                issues.add(QualityCheckIssue(sc.sceneOrder, sc.title, "Narration Too Long", "Scene ${sc.sceneOrder} is ${sc.durationSeconds}s but narration needs ~${need.toInt() + 1}s.", "Increase the scene duration."))
            }
        }
        check(timingOk)

        // 5. Protected terminology survives in narration and subtitles
        val terms = tutorial.customTerminology.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        var termsOk = true
        scenes.forEach { sc ->
            terms.forEach { t ->
                val variantInText = listOf(sc.narrationScript, sc.subtitleText).any { txt ->
                    Regex(Regex.escape(t), RegexOption.IGNORE_CASE).findAll(txt).any { it.value != t }
                }
                val droppedInSubtitle = sc.narrationScript.contains(t) && sc.subtitleText.isNotBlank() && !sc.subtitleText.contains(t)
                if (variantInText || droppedInSubtitle) {
                    termsOk = false
                    issues.add(QualityCheckIssue(sc.sceneOrder, sc.title, "Protected Term Altered", "'$t' is altered or missing in scene ${sc.sceneOrder} text.", "Restore the exact term."))
                }
            }
        }
        check(termsOk)

        // 6. Audio
        if (!audioAvailable) issues.add(QualityCheckIssue(0, "Narration audio", "No Speech Engine", "No text-to-speech voice is available for the narration language; the exported video is silent.", "Install a TTS voice for this language in Android settings or choose another narration language."))
        check(audioAvailable)

        return QualityCheckReport(issues.isEmpty(), passed, total, issues)
    }
}
