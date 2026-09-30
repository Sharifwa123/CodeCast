package com.example.analysis

import com.example.data.model.CodebaseFile
import com.example.data.model.TutorialStepEntity
import com.example.record.*
import org.junit.Assert.*
import org.junit.Test

class RecordPlanningTest {
    private fun file(path: String, content: String) = CodebaseFile(path, "code", content.lines().size, content.length.toLong(), content)
    private val files = Fixtures.read("shop-main", *Fixtures.nextApp).files
    private val wf = CodebaseAnalyzer.analyze(files).findWorkflow("signup:")!!
    private val steps = wf.defaultSteps.mapIndexed { i, s ->
        TutorialStepEntity(id = i + 1, tutorialId = 1, stepOrder = i + 1, title = s.title, screenName = s.screenName, actionType = s.actionType,
            instruction = s.defaultInstruction, verificationStatus = s.verificationStatus, evidenceSource = s.evidenceSource, evidenceElement = s.evidenceElement,
            codeSnippet = s.codeSnippet, codeFilePath = s.codeFilePath)
    }

    @Test fun signUpPlanNavigatesTypesAndOnlyHoversSubmitByDefault() {
        val plan = steps.map { StepPlanner.plan(it, "https://shop.example.io/", false) }
        val nav = plan[0] as PlannedAction.Navigate
        assertEquals("https://shop.example.io/signup", nav.url)
        val email = plan[1] as PlannedAction.Type
        assertEquals("email", email.spec.name); assertEquals("Work email", email.spec.placeholder); assertEquals("demo@example.com", email.text)
        assertEquals("Demo#12345", (plan[2] as PlannedAction.Type).text)
        val click = plan[3] as PlannedAction.Click
        assertFalse("submit must not really click unless the user opted in", click.real)
        assertEquals("Create account", click.spec.text); assertTrue(click.spec.submit)
        assertTrue((StepPlanner.plan(steps[3], "https://x.io", true) as PlannedAction.Click).real)
    }

    @Test fun routesWithIdsAreSkippedNotGuessed() {
        val order = CodebaseAnalyzer.analyze(files).workflows.first { it.id.contains("orders") }
        val s = order.defaultSteps.first { it.actionType == "Navigate" }
        val step = TutorialStepEntity(id = 1, tutorialId = 1, stepOrder = 1, title = s.title, screenName = s.screenName, actionType = s.actionType,
            instruction = s.defaultInstruction, verificationStatus = s.verificationStatus)
        assertTrue(StepPlanner.plan(step, "https://x.io", false) is PlannedAction.Skip)
    }

    @Test fun liveUrlIsFoundInSourceAndIgnoresBadges() {
        val found = LiveUrlFinder.fromSource(listOf(
            file("package.json", """{"name":"x","homepage":"https://shop.acme.io"}"""),
            file("README.md", "![b](https://img.shields.io/x.svg)\nSee docs at https://nextjs.org/docs\nLive demo: https://demo.acme.app/\n"),
            file(".env.example", "NEXT_PUBLIC_SITE_URL=https://www.acme.com\nAPI=http://localhost:3000")
        ), "https://github.com/Acme/shop")
        val urls = found.map { it.url }
        assertTrue(urls.containsAll(listOf("https://shop.acme.io", "https://demo.acme.app", "https://www.acme.com", "https://acme.github.io/shop")))
        assertTrue(urls.none { it.contains("shields") || it.contains("nextjs") || it.contains("localhost") })
        assertEquals("package.json homepage", found.first().source)
    }

    @Test fun urlValidationAndDevices() {
        assertTrue(LiveUrlFinder.isValid("https://a.io")); assertFalse(LiveUrlFinder.isValid("not a url")); assertFalse(LiveUrlFinder.isValid("ftp://a.io"))
        DeviceProfile.values().forEach {
            assertEquals(it.name, 0, it.outWidth % 16); assertEquals(it.name, 0, it.outHeight % 16)
            assertTrue(it.name, it.contentHeight > it.outHeight / 2)
        }
        assertEquals(DeviceProfile.MACOS, DeviceProfile.fromId("MACOS")); assertEquals(DeviceProfile.ANDROID_PHONE, DeviceProfile.fromId("??"))
        assertTrue(DeviceProfile.IPHONE.userAgent.contains("iPhone")); assertTrue(DeviceProfile.LINUX.userAgent.contains("X11"))
        assertEquals(setOf("android", "ios", "windows", "macos", "linux"), DeviceProfile.values().map { it.family }.toSet())
    }
}
