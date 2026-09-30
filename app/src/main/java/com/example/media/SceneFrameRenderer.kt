package com.example.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.GeneratedSceneEntity

data class RenderOptions(
    val width: Int = 1280,
    val height: Int = 720,
    val fps: Int = 10,
    val showSubtitles: Boolean = true,
    val watermark: String? = "CodeCast",
    val fade: Boolean = true,
    val presenter: Bitmap? = null,
    val presenterRight: Boolean = true
)

/** Draws one video frame for a scene from its real code evidence, narration and callout. */
class SceneFrameRenderer(private val o: RenderOptions) {
    private val w = o.width.toFloat()
    private val h = o.height.toFloat()
    private val mono = Typeface.MONOSPACE
    private val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, Color.parseColor("#0B1020"), Color.parseColor("#111A33"), Shader.TileMode.CLAMP) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val kw = Regex("""\b(import|export|from|default|function|const|let|var|return|async|await|if|else|class|def|new|public|private|fun|val|interface|type|extends|final|void|static|use|Route|final)\b""")
    private val str = Regex("""(["'`])(?:(?!\1).)*\1""")

    fun render(bitmap: Bitmap, scene: GeneratedSceneEntity, index: Int, total: Int, tInScene: Float, globalProgress: Float) {
        val c = Canvas(bitmap)
        c.drawRect(0f, 0f, w, h, bg)
        val pad = w * 0.02f
        val codeW = w * 0.62f
        drawCode(c, scene, pad, pad + 34f, codeW - pad, h - 150f, tInScene)
        drawInfo(c, scene, index, total, codeW + pad, pad + 34f, w - pad, h - 150f)
        drawHeader(c, scene, pad)
        if (o.showSubtitles && scene.subtitleText.isNotBlank()) drawSubtitle(c, scene.subtitleText)
        // progress bar
        fill.color = Color.parseColor("#1E293B"); c.drawRect(0f, h - 6f, w, h, fill)
        fill.color = Color.parseColor("#6366F1"); c.drawRect(0f, h - 6f, w * globalProgress.coerceIn(0f, 1f), h, fill)
        o.presenter?.let { drawPresenter(c, it) }
        if (o.fade && tInScene < 0.35f) {
            fill.color = Color.argb(((1f - tInScene / 0.35f) * 255).toInt().coerceIn(0, 255), 11, 16, 32)
            c.drawRect(0f, 0f, w, h, fill)
        }
    }

    private fun drawHeader(c: Canvas, scene: GeneratedSceneEntity, pad: Float) {
        text.reset(); text.isAntiAlias = true
        o.watermark?.let {
            text.color = Color.argb(170, 226, 232, 240); text.textSize = 22f; text.typeface = Typeface.DEFAULT_BOLD
            c.drawText(it, w - pad - text.measureText(it), pad + 22f, text)
        }
        text.color = Color.parseColor("#94A3B8"); text.textSize = 20f; text.typeface = Typeface.DEFAULT
        c.drawText(scene.calloutText, pad, pad + 20f, text)
    }

    private fun drawCode(c: Canvas, s: GeneratedSceneEntity, l: Float, t: Float, r: Float, b: Float, tIn: Float) {
        fill.color = Color.parseColor("#0F172A"); c.drawRoundRect(RectF(l, t, r, b), 14f, 14f, fill)
        fill.color = Color.parseColor("#1E293B"); c.drawRoundRect(RectF(l, t, r, t + 40f), 14f, 14f, fill); c.drawRect(l, t + 20f, r, t + 40f, fill)
        text.reset(); text.isAntiAlias = true
        text.typeface = mono; text.textSize = 19f; text.color = Color.parseColor("#7DD3FC")
        c.drawText(ellipsize(s.codeFilePath, text, r - l - 24f), l + 14f, t + 27f, text)
        val lines = s.codeSnippet.lines()
        val hl = s.highlightedLines.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        val lineH = 30f
        val maxLines = ((b - t - 56f) / lineH).toInt().coerceAtLeast(1)
        text.textSize = 21f
        val gutter = 52f
        val revealed = ((tIn * 14f).toInt() + 1)
        lines.take(minOf(maxLines, revealed)).forEachIndexed { i, line ->
            val y = t + 46f + (i + 1) * lineH - 8f
            val isHl = (i + 1) in hl
            if (isHl) {
                val pulse = 0.22f + 0.10f * Math.sin(tIn * 5.0).toFloat()
                fill.color = Color.argb((pulse * 255).toInt(), 56, 189, 248)
                c.drawRect(l + 4f, y - lineH + 8f, r - 4f, y + 8f, fill)
            }
            text.color = if (isHl) Color.parseColor("#38BDF8") else Color.parseColor("#475569")
            c.drawText("${i + 1}", l + 12f, y, text)
            drawHighlighted(c, ellipsize(line.replace("\t", "  "), text, r - l - gutter - 20f), l + gutter, y)
        }
        // Cursor glides to the highlighted line, then clicks (ripple)
        val hlIdx = (hl.minOrNull() ?: 1) - 1
        if (hlIdx in 0 until minOf(lines.size, maxLines)) {
            val ty = t + 46f + (hlIdx + 1) * lineH - 18f
            val lineText = lines[hlIdx].replace("\t", "  ").trim()
            text.textSize = 21f
            val tx = l + gutter + minOf(text.measureText(lineText), r - l - gutter - 60f) * 0.6f
            val sx = r - 60f
            val sy = b - 40f
            val p = ((tIn - 0.8f) / 1.2f).coerceIn(0f, 1f)
            val e = p * p * (3 - 2 * p)
            val cx = sx + (tx - sx) * e
            val cy = sy + (ty - sy) * e
            if (tIn > 0.8f) {
                if (p >= 1f) {
                    val ph = ((tIn - 2.0f) % 1.3f) / 1.3f
                    fill.style = Paint.Style.STROKE; fill.strokeWidth = 4f
                    fill.color = Color.argb(((1f - ph) * 200).toInt().coerceIn(0, 255), 56, 189, 248)
                    c.drawCircle(cx, cy, 8f + ph * 42f, fill); fill.style = Paint.Style.FILL
                }
                val arrow = Path().apply { moveTo(cx, cy); lineTo(cx + 22f, cy + 10f); lineTo(cx + 11f, cy + 13f); lineTo(cx + 6f, cy + 24f); close() }
                fill.color = Color.WHITE; c.drawPath(arrow, fill)
                fill.style = Paint.Style.STROKE; fill.strokeWidth = 2f; fill.color = Color.parseColor("#0F172A"); c.drawPath(arrow, fill); fill.style = Paint.Style.FILL
            }
        }
    }

    private fun drawHighlighted(c: Canvas, line: String, x: Float, y: Float) {
        val colors = arrayOfNulls<Int>(line.length)
        str.findAll(line).forEach { m -> for (i in m.range) colors[i] = Color.parseColor("#86EFAC") }
        kw.findAll(line).forEach { m -> for (i in m.range) if (colors[i] == null) colors[i] = Color.parseColor("#C4B5FD") }
        val ci = line.indexOf("//")
        if (ci >= 0) for (i in ci until line.length) colors[i] = Color.parseColor("#64748B")
        var cx = x
        var i = 0
        while (i < line.length) {
            val col = colors[i]
            var j = i
            while (j < line.length && colors[j] == col) j++
            text.color = col ?: Color.parseColor("#E2E8F0")
            val seg = line.substring(i, j)
            c.drawText(seg, cx, y, text)
            cx += text.measureText(seg)
            i = j
        }
    }

    private fun drawInfo(c: Canvas, s: GeneratedSceneEntity, index: Int, total: Int, l: Float, t: Float, r: Float, b: Float) {
        text.reset(); text.isAntiAlias = true
        fill.color = Color.parseColor("#312E81"); c.drawRoundRect(RectF(l, t, l + 170f, t + 38f), 19f, 19f, fill)
        text.color = Color.WHITE; text.textSize = 21f; text.typeface = Typeface.DEFAULT_BOLD
        c.drawText("Step ${index + 1} of $total", l + 18f, t + 26f, text)
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 40f; typeface = Typeface.DEFAULT_BOLD }
        val tl = layout(s.title, title, (r - l).toInt())
        c.save(); c.translate(l, t + 62f); tl.draw(c); c.restore()
        var y = t + 62f + tl.height + 22f
        val meta = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#94A3B8"); textSize = 22f; typeface = Typeface.MONOSPACE }
        if (s.terminalOutput.isNotBlank()) {
            val ml = layout(s.terminalOutput, meta, (r - l).toInt())
            c.save(); c.translate(l, y); ml.draw(c); c.restore(); y += ml.height + 10f
        }
    }

    private fun drawSubtitle(c: Canvas, textStr: String) {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD }
        val boxW = (w * 0.86f).toInt()
        val lay = layout(textStr, p, boxW - 40)
        val bh = lay.height + 24f
        val left = (w - boxW) / 2f
        val top = h - 24f - bh
        fill.color = Color.argb(200, 2, 6, 23); c.drawRoundRect(RectF(left, top, left + boxW, top + bh), 14f, 14f, fill)
        c.save(); c.translate(left + 20f, top + 12f); lay.draw(c); c.restore()
    }

    private fun drawPresenter(c: Canvas, bmp: Bitmap) {
        val d = h * 0.24f
        val cx = if (o.presenterRight) w - d / 2 - 30f else d / 2 + 30f
        val cy = h * 0.5f
        val path = Path().apply { addCircle(cx, cy, d / 2, Path.Direction.CW) }
        c.save(); c.clipPath(path)
        val side = minOf(bmp.width, bmp.height)
        val src = android.graphics.Rect((bmp.width - side) / 2, (bmp.height - side) / 2, (bmp.width + side) / 2, (bmp.height + side) / 2)
        c.drawBitmap(bmp, src, RectF(cx - d / 2, cy - d / 2, cx + d / 2, cy + d / 2), null)
        c.restore()
        fill.style = Paint.Style.STROKE; fill.strokeWidth = 5f; fill.color = Color.parseColor("#6366F1")
        c.drawCircle(cx, cy, d / 2, fill); fill.style = Paint.Style.FILL
    }

    private fun layout(s: String, p: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(s, 0, s.length, p, width.coerceAtLeast(10)).setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(4)
            .setEllipsize(android.text.TextUtils.TruncateAt.END).build()

    private fun ellipsize(s: String, p: Paint, maxW: Float): String {
        if (p.measureText(s) <= maxW) return s
        var n = s.length
        while (n > 1 && p.measureText(s, 0, n) + p.measureText("…") > maxW) n--
        return s.substring(0, n) + "…"
    }
}
