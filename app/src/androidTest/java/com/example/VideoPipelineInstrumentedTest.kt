package com.example

import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.analysis.CodebaseAnalyzer
import com.example.analysis.ScenePlanner
import com.example.analysis.ZipCodebaseReader
import com.example.data.model.TutorialPlanEntity
import com.example.data.model.TutorialStepEntity
import com.example.media.RenderOptions
import com.example.media.VideoExporter
import com.example.media.WavData
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Runs the real analysis -> planning -> MediaCodec/MediaMuxer path on an emulator and inspects the MP4. */
@RunWith(AndroidJUnit4::class)
class VideoPipelineInstrumentedTest {
    private fun zip(): ByteArray {
        val b = ByteArrayOutputStream()
        ZipOutputStream(b).use { z ->
            mapOf(
                "package.json" to """{"dependencies":{"next":"14.0.0","react":"18"}}""",
                "src/app/login/page.tsx" to "export default function Login() {\n  return (\n    <form>\n      <input name=\"email\" type=\"email\" placeholder=\"Email\" />\n      <input name=\"password\" type=\"password\" />\n      <button type=\"submit\">Sign in</button>\n    </form>\n  );\n}\n"
            ).forEach { (n, c) -> z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() }
        }
        return b.toByteArray()
    }

    @Test fun uploadedCodebaseBecomesPlayableMp4WithNarrationTrack() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val files = ZipCodebaseReader.read(ByteArrayInputStream(zip())).files
        val analysis = CodebaseAnalyzer.analyze(files)
        val wf = analysis.findWorkflow("login:")!!
        val steps = wf.defaultSteps.mapIndexed { i, s ->
            TutorialStepEntity(id = i + 1, tutorialId = 1, stepOrder = i + 1, title = s.title, screenName = s.screenName, actionType = s.actionType,
                instruction = s.defaultInstruction, verificationStatus = s.verificationStatus, evidenceSource = s.evidenceSource,
                evidenceElement = s.evidenceElement, codeSnippet = s.codeSnippet, codeFilePath = s.codeFilePath)
        }
        val tutorial = TutorialPlanEntity(id = 1, projectId = 1, title = "Login", tutorialType = "LOGIN", duration = "Quick — 30–60 seconds", subtitleLang = "English")
        val scenes = ScenePlanner.plan(1, steps, tutorial)
        assertEquals(steps.size, scenes.size)
        val total = scenes.sumOf { it.durationSeconds }
        val audio = scenes.associate { sc ->
            sc.sceneOrder to WavData(22050, 1, ShortArray(22050 * 2) { (Math.sin(it * 0.05) * 8000).toInt().toShort() })
        }
        val out = File(ctx.cacheDir, "pipeline_test.mp4")
        val progress = mutableListOf<Float>()
        VideoExporter(RenderOptions(fps = 5)).export(scenes, audio, out) { progress.add(it) }

        assertTrue("mp4 written", out.exists() && out.length() > 10_000)
        assertEquals(1f, progress.last(), 0f)
        val mmr = MediaMetadataRetriever().apply { setDataSource(out.absolutePath) }
        val durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
        assertEquals(total * 1000.0, durMs.toDouble(), 1500.0)
        assertEquals("yes", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
        assertEquals("yes", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
        assertEquals("1280", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        val frame = mmr.getFrameAtTime(1_000_000)
        assertNotNull("decodable frame", frame)
        val ex = MediaExtractor().apply { setDataSource(out.absolutePath) }
        val mimes = (0 until ex.trackCount).map { ex.getTrackFormat(it).getString(android.media.MediaFormat.KEY_MIME)!! }
        assertTrue(mimes.toString(), mimes.any { it.startsWith("video/") } && mimes.any { it.startsWith("audio/") })
    }

    @Test fun silentVideoStillRenders() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val files = ZipCodebaseReader.read(ByteArrayInputStream(zip())).files
        val wf = CodebaseAnalyzer.analyze(files).findWorkflow("login:")!!
        val steps = wf.defaultSteps.mapIndexed { i, s ->
            TutorialStepEntity(id = i + 1, tutorialId = 1, stepOrder = i + 1, title = s.title, screenName = s.screenName, actionType = s.actionType,
                instruction = s.defaultInstruction, verificationStatus = s.verificationStatus, evidenceSource = s.evidenceSource, codeSnippet = s.codeSnippet, codeFilePath = s.codeFilePath)
        }
        val scenes = ScenePlanner.plan(1, steps, TutorialPlanEntity(id = 1, projectId = 1, title = "t", tutorialType = "LOGIN", duration = "Quick — 30–60 seconds"))
        val out = File(ctx.cacheDir, "silent.mp4")
        VideoExporter(RenderOptions(fps = 5)).export(scenes, emptyMap(), out) {}
        val mmr = MediaMetadataRetriever().apply { setDataSource(out.absolutePath) }
        assertEquals("yes", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
    }
}
