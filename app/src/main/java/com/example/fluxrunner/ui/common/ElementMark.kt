package com.example.fluxrunner.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.fluxrunner.model.Element
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ElementMark(
    element: Element,
    modifier: Modifier = Modifier,
    tint: Color = Color(element.primaryColor)
) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.42f

        when (element) {
            Element.FIRE -> {
                val outer = Path().apply {
                    moveTo(c.x, c.y - r)
                    cubicTo(c.x - r * 0.95f, c.y - r * 0.22f, c.x - r * 0.55f, c.y + r * 0.92f, c.x, c.y + r)
                    cubicTo(c.x + r * 0.75f, c.y + r * 0.42f, c.x + r * 0.72f, c.y - r * 0.38f, c.x, c.y - r)
                    close()
                }
                drawPath(outer, tint)
                val inner = Path().apply {
                    moveTo(c.x + r * 0.08f, c.y - r * 0.32f)
                    cubicTo(c.x - r * 0.35f, c.y + r * 0.1f, c.x - r * 0.14f, c.y + r * 0.62f, c.x, c.y + r * 0.72f)
                    cubicTo(c.x + r * 0.36f, c.y + r * 0.36f, c.x + r * 0.36f, c.y - r * 0.02f, c.x + r * 0.08f, c.y - r * 0.32f)
                    close()
                }
                drawPath(inner, Color.White.copy(alpha = 0.78f))
            }
            Element.ICE -> {
                for (i in 0 until 6) {
                    val a = Math.toRadians((i * 60).toDouble())
                    drawLine(
                        tint,
                        c,
                        Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r),
                        strokeWidth = r * 0.16f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(Color.White.copy(alpha = 0.82f), r * 0.16f, c)
            }
            Element.ELECTRIC -> {
                val bolt = Path().apply {
                    moveTo(c.x + r * 0.18f, c.y - r)
                    lineTo(c.x - r * 0.2f, c.y - r * 0.04f)
                    lineTo(c.x + r * 0.25f, c.y - r * 0.02f)
                    lineTo(c.x - r * 0.2f, c.y + r)
                    lineTo(c.x + r * 0.12f, c.y + r * 0.12f)
                    lineTo(c.x - r * 0.28f, c.y + r * 0.08f)
                    close()
                }
                drawPath(bolt, tint)
                drawPath(bolt, Color.White.copy(alpha = 0.32f), style = Stroke(width = r * 0.08f))
            }
            Element.TOXIC -> {
                drawCircle(tint.copy(alpha = 0.24f), r * 0.95f, c, style = Stroke(width = r * 0.12f))
                for (i in 0 until 3) {
                    val a = Math.toRadians((i * 120).toDouble())
                    val p = Offset(c.x + cos(a).toFloat() * r * 0.48f, c.y + sin(a).toFloat() * r * 0.48f)
                    drawCircle(tint, r * 0.34f, p)
                }
                drawCircle(Color.White.copy(alpha = 0.9f), r * 0.18f, c)
            }
        }
    }
}
