package com.example.fluxrunner.engine

import android.graphics.*
import com.example.fluxrunner.model.Element
import kotlin.math.*

/**
 * Draws custom vector element symbols on an Android [Canvas].
 * All symbols are rendered at a given [cx], [cy] center point with the given [radius].
 * No emojis are used anywhere in this file.
 */
object ElementSymbolDrawer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Draw a symbol for the given [element] at screen coordinates ([cx], [cy]) with the given [radius].
     * [alpha] ranges 0..255.
     */
    fun draw(canvas: Canvas, element: Element, cx: Float, cy: Float, radius: Float, alpha: Int = 255) {
        when (element.symbolTag) {
            "EMBER" -> drawEmber(canvas, cx, cy, radius, element.primaryColor, alpha)
            "FROST" -> drawFrost(canvas, cx, cy, radius, element.primaryColor, alpha)
            "VOLT"  -> drawVolt(canvas, cx, cy, radius, element.primaryColor, alpha)
            "BLOOM" -> drawBloom(canvas, cx, cy, radius, element.primaryColor, alpha)
        }
    }

    // ---- EMBER: nested upward-pointing triangles with warm glow ----
    private fun drawEmber(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
        paint.reset()
        paint.isAntiAlias = true
        paint.alpha = alpha

        // outer glow
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.18f
        paint.color = adjustAlpha(color, 0.35f)
        canvas.drawPath(triangle(cx, cy, r * 1.0f, up = true), paint)

        // outer solid triangle
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.12f
        paint.color = setAlpha(color, alpha)
        canvas.drawPath(triangle(cx, cy, r * 0.88f, up = true), paint)

        // inner triangle (inverted)
        paint.strokeWidth = r * 0.10f
        paint.color = setAlpha(Color.WHITE, (alpha * 0.6f).toInt())
        canvas.drawPath(triangle(cx, cy - r * 0.1f, r * 0.42f, up = false), paint)

        // center dot
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.alpha = alpha
        canvas.drawCircle(cx, cy - r * 0.05f, r * 0.10f, paint)
    }

    // ---- FROST: 6-spoke asterisk snowflake ----
    private fun drawFrost(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        // main spokes
        paint.strokeWidth = r * 0.14f
        paint.color = setAlpha(color, alpha)
        for (i in 0 until 6) {
            val angle = i * 60.0 * Math.PI / 180.0
            canvas.drawLine(
                cx, cy,
                cx + cos(angle).toFloat() * r * 0.9f,
                cy + sin(angle).toFloat() * r * 0.9f,
                paint
            )
        }

        // small cross-bars on each spoke at 60% radius
        paint.strokeWidth = r * 0.09f
        paint.color = setAlpha(Color.WHITE, (alpha * 0.7f).toInt())
        for (i in 0 until 6) {
            val angle = i * 60.0 * Math.PI / 180.0
            val bx = cx + cos(angle).toFloat() * r * 0.62f
            val by = cy + sin(angle).toFloat() * r * 0.62f
            val perp = angle + Math.PI / 2.0
            val dl = r * 0.2f
            canvas.drawLine(
                bx - cos(perp).toFloat() * dl,
                by - sin(perp).toFloat() * dl,
                bx + cos(perp).toFloat() * dl,
                by + sin(perp).toFloat() * dl,
                paint
            )
        }

        // center dot
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.alpha = alpha
        canvas.drawCircle(cx, cy, r * 0.12f, paint)
    }

    // ---- VOLT: zigzag lightning bolt ----
    private fun drawVolt(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND

        // outer glow stroke
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.22f
        paint.color = adjustAlpha(color, 0.30f)
        paint.alpha = (alpha * 0.45f).toInt()
        canvas.drawPath(boltPath(cx, cy, r * 0.95f), paint)

        // solid bolt
        paint.style = Paint.Style.FILL
        paint.color = setAlpha(color, alpha)
        canvas.drawPath(boltPath(cx, cy, r * 0.85f), paint)

        // white highlight on top half
        paint.color = setAlpha(Color.WHITE, (alpha * 0.4f).toInt())
        canvas.drawPath(boltPath(cx - r * 0.04f, cy - r * 0.04f, r * 0.45f), paint)
    }

    // ---- BLOOM: atomic orbit rings with nucleus ----
    private fun drawBloom(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE

        // 3 elliptical orbits at different angles
        val angles = listOf(0f, 60f, 120f)
        angles.forEach { angleDeg ->
            paint.strokeWidth = r * 0.09f
            paint.color = setAlpha(color, alpha)
            val matrix = Matrix()
            matrix.setRotate(angleDeg, cx, cy)
            val oval = RectF(cx - r * 0.95f, cy - r * 0.42f, cx + r * 0.95f, cy + r * 0.42f)
            val path = Path().apply { addOval(oval, Path.Direction.CW) }
            path.transform(matrix)
            canvas.drawPath(path, paint)
        }

        // nucleus
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx - r * 0.08f, cy - r * 0.08f, r * 0.28f,
            Color.WHITE, color, Shader.TileMode.CLAMP
        )
        paint.alpha = alpha
        canvas.drawCircle(cx, cy, r * 0.26f, paint)
        paint.shader = null

        // small satellite electron
        paint.style = Paint.Style.FILL
        paint.color = setAlpha(Color.WHITE, (alpha * 0.9f).toInt())
        canvas.drawCircle(cx + r * 0.85f, cy, r * 0.10f, paint)
    }

    // --- Helpers ---

    private fun triangle(cx: Float, cy: Float, r: Float, up: Boolean): Path {
        val path = Path()
        val top = if (up) cy - r else cy + r
        val bot = if (up) cy + r * 0.55f else cy - r * 0.55f
        path.moveTo(cx, top)
        path.lineTo(cx + r * 0.87f, bot)
        path.lineTo(cx - r * 0.87f, bot)
        path.close()
        return path
    }

    private fun boltPath(cx: Float, cy: Float, r: Float): Path {
        // Lightning bolt polygon
        val path = Path()
        path.moveTo(cx + r * 0.18f, cy - r)         // top-right
        path.lineTo(cx - r * 0.12f, cy - r * 0.08f) // middle-left inner
        path.lineTo(cx + r * 0.28f, cy - r * 0.02f) // middle-right inner
        path.lineTo(cx - r * 0.18f, cy + r)          // bottom-left
        path.lineTo(cx + r * 0.12f, cy + r * 0.08f) // middle-right inner
        path.lineTo(cx - r * 0.28f, cy + r * 0.02f) // middle-left inner
        path.close()
        return path
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val a = ((Color.alpha(color) * factor).toInt()).coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun setAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }
}
