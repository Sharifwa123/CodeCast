package com.example.record

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/** Composes one output frame: window/device chrome for the chosen device around the captured page. */
class DeviceFrameRenderer(
    private val profile: DeviceProfile,
    private val host: String,
    private val watermark: String?,
    private val showSubtitles: Boolean,
    private val presenter: Bitmap? = null
) {
    private val w = profile.outWidth
    private val h = profile.outHeight
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val time = java.text.SimpleDateFormat("h:mm", java.util.Locale.US)

    fun render(out: Bitmap, page: Bitmap, subtitle: String, stepIndex: Int, totalSteps: Int) {
        val c = Canvas(out)
        val dark = profile.family == "linux" || profile.family == "windows"
        c.drawColor(if (profile.isMobile) Color.BLACK else if (dark) Color.parseColor("#202124") else Color.parseColor("#E8E8EA"))
        val top = profile.chromeTop
        val content = Rect(0, top, w, top + profile.contentHeight)
        c.drawBitmap(page, Rect(0, 0, page.width, page.height), content, null)
        when (profile.family) {
            "macos" -> drawMac(c, top)
            "windows" -> drawWindows(c, top)
            "linux" -> drawLinux(c, top)
            "ios" -> drawIos(c, top)
            else -> drawAndroid(c, top)
        }
        if (showSubtitles && subtitle.isNotBlank()) drawSubtitle(c, subtitle, content)
        // step progress
        fill.color = Color.parseColor("#33000000"); c.drawRect(content.left.toFloat(), content.bottom - 6f, content.right.toFloat(), content.bottom.toFloat(), fill)
        fill.color = Color.parseColor("#6366F1")
        c.drawRect(content.left.toFloat(), content.bottom - 6f, content.left + content.width() * (stepIndex + 1f) / totalSteps.coerceAtLeast(1), content.bottom.toFloat(), fill)
        watermark?.let {
            text.reset(); text.isAntiAlias = true; text.color = Color.argb(200, 255, 255, 255); text.textSize = 20f; text.typeface = Typeface.DEFAULT_BOLD
            val tw = text.measureText(it)
            fill.color = Color.argb(140, 15, 23, 42); c.drawRoundRect(RectF(w - tw - 34f, content.top + 10f, w - 10f, content.top + 44f), 10f, 10f, fill)
            c.drawText(it, w - tw - 22f, content.top + 33f, text)
        }
        presenter?.let { drawPresenter(c, it, content) }
    }

    private fun urlPill(c: Canvas, r: RectF, dark: Boolean) {
        fill.color = if (dark) Color.parseColor("#3C4043") else Color.WHITE
        c.drawRoundRect(r, r.height() / 2, r.height() / 2, fill)
        text.reset(); text.isAntiAlias = true; text.textSize = r.height() * 0.46f
        text.color = if (dark) Color.parseColor("#E8EAED") else Color.parseColor("#3C4043")
        val label = "🔒 $host"
        var s = label
        while (s.length > 4 && text.measureText(s) > r.width() - 30f) s = s.dropLast(2)
        c.drawText(if (s == label) s else "$s…", r.left + 18f, r.centerY() + text.textSize * 0.35f, text)
    }

    private fun drawMac(c: Canvas, top: Int) {
        fill.color = Color.parseColor("#F2F2F4"); c.drawRect(0f, 0f, w.toFloat(), top.toFloat(), fill)
        listOf("#FF5F57", "#FEBC2E", "#28C840").forEachIndexed { i, col -> fill.color = Color.parseColor(col); c.drawCircle(34f + i * 30f, top / 2f, 9f, fill) }
        urlPill(c, RectF(w * 0.22f, top / 2f - 22f, w * 0.78f, top / 2f + 22f), false)
        fill.color = Color.parseColor("#D0D0D4"); c.drawRect(0f, top - 1f, w.toFloat(), top.toFloat(), fill)
    }

    private fun drawWindows(c: Canvas, top: Int) {
        fill.color = Color.parseColor("#DEE1E6"); c.drawRect(0f, 0f, w.toFloat(), 46f, fill)
        fill.color = Color.WHITE; c.drawRoundRect(RectF(12f, 8f, 300f, 46f), 10f, 10f, fill)
        text.reset(); text.isAntiAlias = true; text.textSize = 18f; text.color = Color.parseColor("#202124"); c.drawText(host.take(24), 30f, 33f, text)
        text.textSize = 26f; text.color = Color.parseColor("#202124")
        c.drawText("—", w - 150f, 32f, text); c.drawText("▢", w - 100f, 32f, text); c.drawText("✕", w - 46f, 32f, text)
        fill.color = Color.WHITE; c.drawRect(0f, 46f, w.toFloat(), top.toFloat(), fill)
        urlPill(c, RectF(90f, 54f, w - 90f, top - 8f), false)
    }

    private fun drawLinux(c: Canvas, top: Int) {
        fill.color = Color.parseColor("#2D2D30"); c.drawRect(0f, 0f, w.toFloat(), 44f, fill)
        text.reset(); text.isAntiAlias = true; text.textSize = 20f; text.color = Color.parseColor("#F0F0F0"); text.typeface = Typeface.DEFAULT_BOLD
        c.drawText(host, w / 2f - text.measureText(host) / 2, 30f, text)
        fill.color = Color.parseColor("#E95420"); c.drawCircle(w - 30f, 22f, 12f, fill)
        fill.color = Color.parseColor("#3A3A3D"); c.drawRect(0f, 44f, w.toFloat(), top.toFloat(), fill)
        urlPill(c, RectF(80f, 50f, w - 80f, top - 6f), true)
    }

    private fun statusBar(c: Canvas, dark: Boolean) {
        text.reset(); text.isAntiAlias = true; text.textSize = 26f; text.typeface = Typeface.DEFAULT_BOLD
        text.color = if (dark) Color.WHITE else Color.BLACK
        c.drawText(time.format(java.util.Date()), 36f, 44f, text)
        c.drawText("5G  ▮▮▮", w - 150f, 44f, text)
    }

    private fun drawIos(c: Canvas, top: Int) {
        fill.color = Color.parseColor("#F7F7F9"); c.drawRect(0f, 0f, w.toFloat(), top.toFloat(), fill)
        statusBar(c, false)
        if (profile.cssWidth == null) { fill.color = Color.BLACK; c.drawRoundRect(RectF(w / 2f - 70f, 12f, w / 2f + 70f, 52f), 20f, 20f, fill) }
        val bottomTop = (profile.outHeight - profile.chromeBottom).toFloat()
        fill.color = Color.parseColor("#F7F7F9"); c.drawRect(0f, bottomTop, w.toFloat(), h.toFloat(), fill)
        if (profile.cssWidth == null) {
            urlPill(c, RectF(30f, bottomTop + 12f, w - 30f, bottomTop + 70f), false)
            fill.color = Color.BLACK; c.drawRoundRect(RectF(w / 2f - 90f, h - 20f, w / 2f + 90f, h - 13f), 4f, 4f, fill)
        } else urlPill(c, RectF(w * 0.25f, 46f, w * 0.75f, top - 8f), false)
    }

    private fun drawAndroid(c: Canvas, top: Int) {
        fill.color = Color.parseColor("#F1F3F4"); c.drawRect(0f, 0f, w.toFloat(), top.toFloat(), fill)
        statusBar(c, false)
        urlPill(c, RectF(24f, 64f, w - 24f, top - 10f), false)
        val bottomTop = (profile.outHeight - profile.chromeBottom).toFloat()
        fill.color = Color.BLACK; c.drawRect(0f, bottomTop, w.toFloat(), h.toFloat(), fill)
        fill.color = Color.parseColor("#DDDDDD"); c.drawRoundRect(RectF(w / 2f - 70f, h - 26f, w / 2f + 70f, h - 20f), 4f, 4f, fill)
    }

    private fun drawSubtitle(c: Canvas, s: String, content: Rect) {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = if (profile.isMobile && profile.cssWidth == null) 30f else 30f; typeface = Typeface.DEFAULT_BOLD }
        val bw = (content.width() * 0.9f).toInt()
        val lay = StaticLayout.Builder.obtain(s, 0, s.length, p, bw - 36).setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(3)
            .setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        val bh = lay.height + 22f
        val left = (w - bw) / 2f
        val topY = content.bottom - 28f - bh
        fill.color = Color.argb(205, 2, 6, 23); c.drawRoundRect(RectF(left, topY, left + bw, topY + bh), 14f, 14f, fill)
        c.save(); c.translate(left + 18f, topY + 11f); lay.draw(c); c.restore()
    }

    private fun drawPresenter(c: Canvas, bmp: Bitmap, content: Rect) {
        val d = content.height() * 0.2f
        val cx = w - d / 2 - 24f
        val cy = content.bottom - d / 2 - 110f
        val path = Path().apply { addCircle(cx, cy, d / 2, Path.Direction.CW) }
        c.save(); c.clipPath(path)
        val side = minOf(bmp.width, bmp.height)
        c.drawBitmap(bmp, Rect((bmp.width - side) / 2, (bmp.height - side) / 2, (bmp.width + side) / 2, (bmp.height + side) / 2), RectF(cx - d / 2, cy - d / 2, cx + d / 2, cy + d / 2), null)
        c.restore()
        fill.style = Paint.Style.STROKE; fill.strokeWidth = 5f; fill.color = Color.parseColor("#6366F1"); c.drawCircle(cx, cy, d / 2, fill); fill.style = Paint.Style.FILL
    }
}
