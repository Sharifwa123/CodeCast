package com.example

import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.media.WavData
import com.example.media.encodeAacPcm
import com.example.record.DeviceProfile
import com.example.record.ElementSpec
import com.example.record.PlannedAction
import com.example.record.RecordingSession
import com.example.record.WebRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Records a real WebView while the director types and clicks, then checks the resulting MP4 actually moved. */
@RunWith(AndroidJUnit4::class)
class WebRecordingInstrumentedTest {
    private val html = """<html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="font-family:sans-serif;padding:16px;background:#fff;color:#111;font-size:22px">
<h1>Sign up</h1>
<form onsubmit="window.__submitted=true;return false">
  <label>Work email<br><input name="email" type="email" placeholder="Work email" style="font-size:22px;width:90%"></label><br><br>
  <label>Password<br><input name="password" type="password" style="font-size:22px;width:90%"></label><br><br>
  <button type="submit" id="go" style="font-size:22px;padding:10px 20px;background:#4f46e5;color:#fff;border:0">Create account</button>
</form></body></html>"""

    private fun distinctColors(b: Bitmap): Int {
        val seen = HashSet<Int>()
        for (y in 0 until b.height step 6) for (x in 0 until b.width step 6) seen.add(b.getPixel(x, y))
        return seen.size
    }

    @Test fun realWebViewIsRecordedWithTypingCursorAndNarration() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        lateinit var webView: WebView
        val profile = DeviceProfile.ANDROID_PHONE
        scenario.onActivity { act ->
            webView = WebView(act)
            val w = 720
            act.setContentView(webView, FrameLayout.LayoutParams(w, (w * profile.contentAspect).toInt()))
        }
        val recorder = WebRecorder(webView, profile)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { recorder.configure() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            runBlocking {
                assertTrue("page loads", recorder.loadHtml(html, "https://shop.test/"))

                // 1. the WebView can really be captured (not a blank surface)
                val probe = Bitmap.createBitmap(profile.contentWidth, profile.contentHeight, Bitmap.Config.ARGB_8888)
                recorder.capture(probe)
                assertTrue("captured page is not blank: ${distinctColors(probe)} colors", distinctColors(probe) > 4)

                // 2. record while the director works
                val out = File(ctx.cacheDir, "web_recording.mp4")
                val session = RecordingSession(recorder, profile, out, "shop.test", "CodeCast", true, null)
                session.subtitle = "Type your work email"; session.totalSteps = 2
                session.start(scope)

                val email = ElementSpec(kind = "input", name = "email", type = "email", placeholder = "Work email", label = "Work email")
                assertTrue("email field found", recorder.probe(email))
                val typed = recorder.run(PlannedAction.Type(email, "demo@example.com"))
                assertTrue("typed: $typed", typed.found && typed.ok)
                assertEquals("\"demo@example.com\"", recorder.evalString("document.querySelector('[name=email]').value"))

                session.step = 1; session.subtitle = "Choose Create account"
                val button = ElementSpec(kind = "button", text = "Create account", submit = true)
                assertTrue("button found", recorder.probe(button))
                val safe = recorder.run(PlannedAction.Click(button, false))
                assertTrue(safe.found && safe.ok && !safe.clicked)
                assertEquals("safe mode must not submit the form", "null", recorder.evalString("String(window.__submitted === undefined ? null : window.__submitted)").replace("\"", "").let { if (it == "null") "null" else it })

                val missing = ElementSpec(kind = "input", name = "nope", label = "Nope", type = "text")
                assertFalse("missing elements are reported, not faked", recorder.probe(missing))

                // 3. narration track (1 s tone) aligned into the recording
                val rate = 22050
                val pcm = ShortArray(rate * 8)
                for (i in 0 until rate) pcm[i] = (Math.sin(i * 0.06) * 9000).toInt().toShort()
                val aac = encodeAacPcm(pcm, rate)
                val file = session.finish(aac)

                // 4. verify the MP4
                assertTrue(file.exists() && file.length() > 20_000)
                assertTrue("enough frames: ${session.frameCount}", session.frameCount >= 12)
                val mmr = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
                val durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                assertTrue("duration $durMs ms", durMs in 2500..30000)
                assertEquals("yes", mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
                assertEquals(profile.outWidth.toString(), mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
                val early = mmr.getFrameAtTime(200_000, MediaMetadataRetriever.OPTION_CLOSEST)
                val late = mmr.getFrameAtTime((durMs - 400) * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                assertNotNull(early); assertNotNull(late)
                var diff = 0
                for (y in 0 until early!!.height step 4) for (x in 0 until early.width step 4) if (early.getPixel(x, y) != late!!.getPixel(x, y)) diff++
                assertTrue("frames must differ (typing, cursor, subtitle): $diff", diff > 300)
                val ex = MediaExtractor().apply { setDataSource(file.absolutePath) }
                val mimes = (0 until ex.trackCount).map { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)!! }
                assertTrue(mimes.toString(), mimes.any { it.startsWith("video/") } && mimes.any { it.startsWith("audio/") })
            }
        } finally {
            scope.cancel()
            scenario.close()
        }
    }
}
