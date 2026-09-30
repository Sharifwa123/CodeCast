package com.example.record

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaFormat
import android.os.SystemClock
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.media.AudioSample
import com.example.media.FrameEncoder
import com.example.media.Mux
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

data class ActionResult(val ok: Boolean, val found: Boolean, val clicked: Boolean)

/** Drives a real WebView (page load, typing, clicks) that is on screen so it can be captured as video. */
class WebRecorder(private val webView: WebView, private val profile: DeviceProfile) {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private var loadSignal: CompletableDeferred<Boolean>? = null
    @Volatile var lastError: String? = null

    private inner class Bridge {
        @JavascriptInterface fun done(token: String, json: String) { pending.remove(token)?.complete(json) }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    fun configure() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = profile.userAgent
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            textZoom = 100
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        webView.addJavascriptInterface(Bridge(), "CCBridge")
        webView.setBackgroundColor(android.graphics.Color.WHITE)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                profile.cssWidth?.let { wpx ->
                    view.evaluateJavascript("(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.content='width=$wpx';})()", null)
                }
                view.evaluateJavascript(DirectorScript.JS, null)
                loadSignal?.complete(true)
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) { lastError = "${error.description}"; loadSignal?.complete(false) }
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame && response.statusCode >= 400) { lastError = "HTTP ${response.statusCode}"; loadSignal?.complete(false) }
            }
        }
    }

    /** Loads [url] and waits until it has finished and had a moment to render. */
    suspend fun load(url: String): Boolean {
        lastError = null
        val signal = CompletableDeferred<Boolean>()
        loadSignal = signal
        withContext(Dispatchers.Main) { webView.loadUrl(url) }
        val ok = withTimeoutOrNull(35_000) { signal.await() } ?: run { lastError = "Timed out loading $url"; false }
        if (ok) delay(1600)
        return ok
    }

    /** Loads inline HTML (used by tests, no network needed). */
    suspend fun loadHtml(html: String, baseUrl: String): Boolean {
        lastError = null
        val signal = CompletableDeferred<Boolean>()
        loadSignal = signal
        withContext(Dispatchers.Main) { webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null) }
        val ok = withTimeoutOrNull(20_000) { signal.await() } ?: false
        if (ok) delay(800)
        return ok
    }

    suspend fun evalString(js: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<String> { cont -> webView.evaluateJavascript(js) { r -> if (cont.isActive) cont.resume(r ?: "null") } }
    }

    /** Reads the visible fields and buttons of the page currently shown. Re-scans until the page stops changing. */
    suspend fun scan(route: String, url: String): LiveScan {
        var last: LiveScan? = null
        repeat(4) { attempt ->
            val raw = evalString("(window.__cc?window.__cc.scan():null)")
            val json = try { (org.json.JSONTokener(raw).nextValue() as? String) } catch (_: Exception) { null }
            if (json != null) {
                val o = JSONObject(json)
                val arr = o.getJSONArray("elements")
                val els = (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    LiveElement(e.getString("kind"), e.optString("tag"), e.optString("type"), e.optString("name"), e.optString("id"),
                        e.optString("placeholder"), e.optString("label"), e.optString("text"), e.getInt("index"), e.optBoolean("inForm"), e.optBoolean("required"))
                }
                val scan = LiveScan(route, o.optString("url", url), o.optString("title"), els)
                if (last != null && last!!.elements.size == els.size && els.isNotEmpty()) return scan
                last = scan
            }
            delay(1200)
        }
        return last ?: LiveScan(route, url, "", emptyList(), "page scripts could not run")
    }

    suspend fun probe(spec: ElementSpec): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Boolean> { cont ->
            webView.evaluateJavascript("(window.__cc?window.__cc.probe(${specJson(spec)}):false)") { r -> if (cont.isActive) cont.resume(r == "true") }
        }
    }

    suspend fun run(action: PlannedAction): ActionResult {
        val (op, spec, text, real) = when (action) {
            is PlannedAction.Type -> listOf("type", action.spec, action.text, false)
            is PlannedAction.Click -> listOf("click", action.spec, "", action.real)
            else -> return ActionResult(true, true, false)
        }
        val token = UUID.randomUUID().toString()
        val d = CompletableDeferred<String>()
        pending[token] = d
        val a = JSONObject().put("op", op).put("spec", JSONObject(specJson(spec as ElementSpec))).put("text", text).put("real", real)
        withContext(Dispatchers.Main) { webView.evaluateJavascript("window.__cc && window.__cc.run('$token', $a);", null) }
        val json = withTimeoutOrNull(30_000) { d.await() }
        pending.remove(token)
        if (json == null) return ActionResult(false, false, false)
        val o = JSONObject(json)
        return ActionResult(o.optBoolean("ok"), o.optBoolean("found"), o.optBoolean("clicked"))
    }

    fun specJson(s: ElementSpec): String = JSONObject()
        .put("kind", s.kind).put("name", s.name).put("id", s.id).put("placeholder", s.placeholder)
        .put("type", s.type).put("label", s.label).put("text", s.text).put("submit", s.submit).put("index", s.index).toString()

    /** Draws the current WebView contents (scaled) into [dst]. Must run on the main thread. */
    fun capture(dst: Bitmap) {
        val c = Canvas(dst)
        val vw = webView.width.coerceAtLeast(1)
        val vh = webView.height.coerceAtLeast(1)
        c.drawColor(android.graphics.Color.WHITE)
        c.save()
        c.scale(dst.width / vw.toFloat(), dst.height / vh.toFloat())
        webView.draw(c)
        c.restore()
    }
}

class RecordedFrame(val page: Bitmap, val ptsUs: Long, val subtitle: String, val step: Int, val total: Int, val clip: File? = null, val clipStartUs: Long = 0)

/** Captures the WebView at a steady rate while the script runs, encodes to video, then muxes narration in. */
class RecordingSession(
    private val recorder: WebRecorder,
    private val profile: DeviceProfile,
    private val outFile: File,
    private val host: String,
    private val watermark: String?,
    private val showSubtitles: Boolean,
    private val presenter: Bitmap?,
    private val fps: Int = 12
) {
    private val tmp = File(outFile.parentFile, outFile.nameWithoutExtension + "_video.mp4")
    private val frames = Channel<RecordedFrame>(3)
    private var startNs = 0L
    private var captureJob: Job? = null
    private var encodeJob: Job? = null
    @Volatile var subtitle: String = ""
    @Volatile var presenterClip: File? = null
    @Volatile var presenterClipStartUs: Long = 0
    @Volatile var step: Int = 0
    @Volatile var totalSteps: Int = 1
    @Volatile var frameCount = 0
    @Volatile var encodeError: Throwable? = null

    val elapsedSec: Double get() = (SystemClock.elapsedRealtimeNanos() - startNs) / 1e9

    fun start(scope: CoroutineScope) {
        startNs = SystemClock.elapsedRealtimeNanos()
        val encoder = FrameEncoder(profile.outWidth, profile.outHeight, fps, tmp)
        val renderer = DeviceFrameRenderer(profile, host, watermark, showSubtitles, presenter)
        encodeJob = scope.launch(Dispatchers.Default) {
            val out = Bitmap.createBitmap(profile.outWidth, profile.outHeight, Bitmap.Config.ARGB_8888)
            val retrievers = HashMap<String, android.media.MediaMetadataRetriever>()
            try {
                for (f in frames) {
                    var face: Bitmap? = null
                    try {
                        face = f.clip?.let { clip ->
                            try {
                                retrievers.getOrPut(clip.path) { android.media.MediaMetadataRetriever().apply { setDataSource(clip.path) } }
                                    .getFrameAtTime((f.ptsUs - f.clipStartUs).coerceAtLeast(0), android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                            } catch (_: Exception) { null }
                        }
                        renderer.render(out, f.page, f.subtitle, f.step, f.total, face)
                        encoder.addFrame(out, f.ptsUs)
                    } catch (t: Throwable) { encodeError = t }
                    face?.recycle()
                    f.page.recycle()
                }
            } finally {
                retrievers.values.forEach { try { it.release() } catch (_: Exception) {} }
                out.recycle()
                try { encoder.finish() } catch (t: Throwable) { encodeError = encodeError ?: t }
            }
        }
        captureJob = scope.launch(Dispatchers.Main) {
            val interval = 1000L / fps
            while (true) {
                val t0 = SystemClock.elapsedRealtime()
                val bmp = Bitmap.createBitmap(profile.contentWidth, profile.contentHeight, Bitmap.Config.ARGB_8888)
                recorder.capture(bmp)
                val pts = (SystemClock.elapsedRealtimeNanos() - startNs) / 1000
                if (frames.trySend(RecordedFrame(bmp, pts, subtitle, step, totalSteps, presenterClip, presenterClipStartUs)).isSuccess) frameCount++ else bmp.recycle()
                delay((interval - (SystemClock.elapsedRealtime() - t0)).coerceAtLeast(1))
            }
        }
    }

    /** Stops capture, finishes the video and adds the narration track. */
    suspend fun finish(audio: Pair<MediaFormat, List<AudioSample>>?): File {
        captureJob?.cancel(); captureJob?.join()
        frames.close()
        encodeJob?.join()
        encodeError?.let { throw IllegalStateException("Video encoding failed: ${it.message}", it) }
        check(frameCount > 0) { "No frames were captured from the page" }
        withContext(Dispatchers.Default) { Mux.combine(tmp, audio, outFile); tmp.delete() }
        return outFile
    }

    fun abort() {
        captureJob?.cancel(); frames.close(); encodeJob?.cancel(); tmp.delete()
    }
}
