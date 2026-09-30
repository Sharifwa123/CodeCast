package com.example.analysis

import com.example.data.model.TutorialStepEntity
import com.example.record.*
import org.junit.Assert.*
import org.junit.Test

class ReconcilerTest {
    private val files = Fixtures.read("shop-main", *Fixtures.nextApp).files
    private val wf = CodebaseAnalyzer.analyze(files).findWorkflow("signup:")!!
    private val steps = wf.defaultSteps.mapIndexed { i, s ->
        TutorialStepEntity(id = i + 1, tutorialId = 1, stepOrder = i + 1, title = s.title, screenName = s.screenName, actionType = s.actionType,
            instruction = s.defaultInstruction, verificationStatus = s.verificationStatus, evidenceSource = s.evidenceSource, evidenceElement = s.evidenceElement,
            codeSnippet = s.codeSnippet, codeFilePath = s.codeFilePath)
    }

    private fun el(kind: String, idx: Int, type: String = "text", name: String = "", ph: String = "", label: String = "", text: String = "", form: Boolean = true) =
        LiveElement(kind, if (kind == "input") "input" else "button", type, name, "", ph, label, text, idx, form)

    private val liveSignup = LiveScan("/signup", "https://shop.io/signup", "Sign up", listOf(
        el("input", 0, "email", "email", "you@company.com", "Business email"),
        el("input", 1, "password", "password", "At least 8 characters", "Password"),
        el("input", 2, "text", "company", "Acme", "Company name"),
        el("input", 3, "checkbox", "terms", "", "I agree"),
        el("button", 0, "submit", "", "", "", "Create workspace"),
        el("button", 1, "button", "", "", "", "Log in", form = false)
    ))

    @Test fun codeAndLivePageAreMatchedAndCorrectedFromWhatTheUserActuallySees() {
        val r = Reconciler.reconcile(steps, mapOf("/" to liveSignup, "/signup" to liveSignup))
        val titles = r.steps.map { it.title }
        assertEquals(listOf("Open Signup", "Enter business email", "Enter your password", "Enter company name", "Click Create workspace"), titles)
        assertTrue(r.steps.all { it.isChecked })
        assertTrue(r.steps.all { it.verificationStatus == "RUNTIME_VERIFIED" })
        assertEquals(listOf(1, 2, 3, 4, 5), r.steps.map { it.stepOrder })
        // exact live targets are carried to the recorder
        assertEquals(0, r.specs.getValue(r.steps[1].id).index)
        assertEquals("email", r.specs.getValue(r.steps[1].id).name)
        assertEquals(4, r.steps.size - 1)
        assertEquals(listOf("Enter company name"), r.report.liveOnly)
        assertTrue(r.report.codeOnly.isEmpty())
        val live = r.steps.first { it.title == "Enter company name" }
        assertEquals("Live page only", live.evidenceSource)
        assertEquals("Fill the 'Company name' field", live.instruction)
        // checkbox and the unrelated nav button are not turned into steps
        assertTrue(r.steps.none { it.title.contains("agree", true) || it.title.contains("Log in") })
    }

    @Test fun stepsMissingFromTheLivePageAreExcludedAndExplained() {
        val sparse = LiveScan("/signup", "u", "t", listOf(el("input", 0, "email", "email", "", "Email")))
        val r = Reconciler.reconcile(steps, mapOf("/" to sparse, "/signup" to sparse))
        val byTitle = r.steps.associateBy { it.title }
        assertTrue(byTitle.getValue("Enter email").isChecked)
        val pwd = r.steps.first { it.title == "Enter your password" }
        assertFalse(pwd.isChecked)
        assertFalse(r.steps.first { it.actionType == "Click" }.isChecked)
        assertEquals(2, r.report.codeOnly.size)
        assertTrue(r.report.codeOnly.first().second.contains("not on the live page"))
        assertTrue(r.report.codeOnly.first().second.contains("Email"))
    }

    @Test fun unreachablePageOrIdRoutesAreNeverGuessed() {
        val failed = LiveScan("/signup", "u", "", emptyList(), "HTTP 404")
        val r = Reconciler.reconcile(steps, mapOf("/" to failed, "/signup" to failed))
        assertTrue(r.steps.none { it.isChecked })
        assertTrue(r.report.pageNotes.any { it.contains("404") })
        // a route with an id has no scan, so its steps are excluded, not checked against the home page
        val idStep = steps.first().copy(id = 10, title = "Open Order", instruction = "Go to /orders/:id")
        val input = steps[1].copy(id = 11)
        val r2 = Reconciler.reconcile(listOf(idStep, input), mapOf("/" to liveSignup))
        assertFalse(r2.steps.last().isChecked)
    }

    @Test fun similarityHandlesRealWorldLabels() {
        assertTrue(Reconciler.similarity("Work email", "Email") >= 0.5)
        assertTrue(Reconciler.similarity("Create account", "Create your account") >= 0.5)
        assertTrue(Reconciler.similarity("Password", "Email") < 0.5)
    }
}
