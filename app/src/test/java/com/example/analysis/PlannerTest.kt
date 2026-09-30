package com.example.analysis

import com.example.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlannerTest {
    private val files = Fixtures.read("", *Fixtures.nextApp).files
    private val wf = CodebaseAnalyzer.analyze(files).findWorkflow("signup:")!!
    private fun steps() = wf.defaultSteps.mapIndexed { i, s ->
        TutorialStepEntity(id = i + 1, tutorialId = 1, stepOrder = i + 1, title = s.title, screenName = s.screenName, actionType = s.actionType,
            instruction = s.defaultInstruction, verificationStatus = s.verificationStatus, evidenceSource = s.evidenceSource,
            evidenceElement = s.evidenceElement, codeSnippet = s.codeSnippet, codeFilePath = s.codeFilePath)
    }
    private fun tut(duration: String = "Quick — 30–60 seconds", sub: String = "English", terms: String = "") =
        TutorialPlanEntity(id = 1, projectId = 1, title = "t", tutorialType = "SIGN_UP", duration = duration, subtitleLang = sub, customTerminology = terms)

    @Test fun scenesCarryEachStepsOwnEvidenceAndNarration() {
        val st = steps()
        val sc = ScenePlanner.plan(1, st, tut())
        assertEquals(st.size, sc.size)
        sc.forEachIndexed { i, s ->
            assertEquals(st[i].codeFilePath, s.codeFilePath)
            assertEquals(st[i].codeSnippet, s.codeSnippet)
            assertTrue("narration mentions step: ${s.narrationScript}", s.narrationScript.contains(st[i].instruction.take(12)) || s.narrationScript.contains(st[i].title))
        }
        val hl = sc[1].highlightedLines.toInt()
        assertTrue(sc[1].codeSnippet.lines()[hl - 1].contains("name=\"email\""))
    }

    @Test fun uncheckedStepsAreExcluded() {
        val st = steps().mapIndexed { i, s -> if (i == 1) s.copy(isChecked = false) else s }
        assertEquals(st.size - 1, ScenePlanner.plan(1, st, tut()).size)
    }

    @Test fun scenesAreNeverPaddedWithSilence() {
        val st = steps()
        val short = ScenePlanner.plan(1, st, tut("Detailed — 3–7 minutes"))
        // 4 short steps must not be stretched to the 3-minute minimum
        assertTrue("total=${short.sumOf { it.durationSeconds }}", short.sumOf { it.durationSeconds } < 60)
        short.forEach { assertTrue(it.durationSeconds <= Math.ceil(ScenePlanner.spokenSeconds(it.narrationScript, 1f) + 1.0).toInt().coerceAtLeast(3)) }
        // measured TTS length longer than the estimate wins
        val sc = ScenePlanner.plan(1, st, tut(), speechSeconds = { 20.0 })
        assertTrue(sc.all { it.durationSeconds >= 21 })
    }

    @Test fun subtitlesOffMeansNoSubtitleText() {
        assertTrue(ScenePlanner.plan(1, steps(), tut().copy(hasSubtitles = false)).all { it.subtitleText.isEmpty() })
    }

    @Test fun translationIsRealOrReportedMissing() = runBlocking {
        val sc = ScenePlanner.plan(1, steps(), tut(sub = "French"))
        val fake = object : Translator { override suspend fun translate(texts: List<String>, targetLanguage: String) = texts.map { "[$targetLanguage] $it" } }
        val (ok, r1) = ScenePlanner.localize(sc, tut(sub = "French"), fake)
        assertTrue(r1.subtitlesTranslated); assertTrue(ok.all { it.subtitleText.startsWith("[French]") })
        val (none, r2) = ScenePlanner.localize(sc, tut(sub = "French"), null)
        assertFalse(r2.subtitlesTranslated); assertEquals(sc.map { it.narrationScript }, none.map { it.subtitleText })
        val q = QualityChecker.run(steps(), none, tut(sub = "French"), files, r2)
        assertTrue(q.issues.any { it.issueType == "Not Translated" })
    }

    @Test fun qualityPassesForHonestPlan_andFailsWhenBroken() {
        val st = steps(); val t = tut()
        val sc = ScenePlanner.plan(1, st, t)
        val good = QualityChecker.run(st, sc, t, files)
        assertTrue(good.issues.joinToString { it.description }, good.isClean)
        assertEquals(good.totalChecks, good.passedChecks)
        // fabricated evidence is caught
        val bad = QualityChecker.run(st, sc.map { it.copy(codeSnippet = "does not exist in project") }, t, files)
        assertTrue(bad.issues.any { it.issueType == "Missing Evidence" })
        // nothing selected must not pass
        assertFalse(QualityChecker.run(emptyList(), emptyList(), t, files).isClean)
        // unchecked unverified step doesn't count
        val withDead = st + st[0].copy(id = 99, stepOrder = 9, verificationStatus = "UNABLE_TO_VERIFY", isChecked = false)
        assertTrue(QualityChecker.run(withDead, sc, t, files).isClean)
        // no TTS engine is reported
        assertTrue(QualityChecker.run(st, sc, t, files, audioAvailable = false).issues.any { it.issueType == "No Speech Engine" })
    }

    @Test fun protectedTermsMustSurvive() {
        val st = steps().map { it.copy(title = "Configure Paystack", instruction = "Configure Paystack") }
        val t = tut(terms = "Paystack")
        val sc = ScenePlanner.plan(1, st, t)
        assertTrue(QualityChecker.run(st, sc, t, files).issues.none { it.issueType == "Protected Term Altered" })
        val broken = sc.map { it.copy(narrationScript = it.narrationScript.replace("Paystack", "PayStack"), subtitleText = "x") }
        assertTrue(QualityChecker.run(st, broken, t, files).issues.any { it.issueType == "Protected Term Altered" })
    }
}
