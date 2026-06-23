package com.example.fluxrunner.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.PathEffect
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.getValue
import com.example.fluxrunner.logic.SaveManager
import com.example.fluxrunner.model.Element
import com.example.fluxrunner.ui.common.ElementMark

@Composable
fun MainMenuScreen(
    saveManager: SaveManager,
    onPlayClick: () -> Unit,
    onShopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMissionsClick: () -> Unit = {},
    onEarnCashClick: () -> Unit = {},
    onWithdrawCashClick: () -> Unit = {},
    onArenaClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val highScore = saveManager.getHighScore()
    val totalCoins = saveManager.getCoins()
    val totalShards = saveManager.getShards()
    val totalTokens = saveManager.getTokens()
    val totalCash = saveManager.getRealCash()
    val maxDistance = saveManager.getLongestDistance().toInt()

    Box(
        modifier = modifier
            .background(Color(0xFF050813))
            .safeDrawingPadding()
    ) {
        HomeTrackBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopHud(
                coins = totalCoins,
                shards = totalShards,
                tokens = totalTokens,
                cash = totalCash,
                onSettingsClick = onSettingsClick
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FluxLogo()
                Spacer(modifier = Modifier.height(12.dp))
                RunnerShowcase()
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickStats(highScore = highScore, maxDistance = maxDistance)

                PlayButton(onClick = onPlayClick)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DockButton("SKINS", "VAULT", onShopClick, Modifier.weight(1f))
                    DockButton("PVP", "ARENA", onArenaClick, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EarnCashButton(onClick = onEarnCashClick, modifier = Modifier.weight(1f))
                    DockButton("CASH", "WITHDRAW", onWithdrawCashClick, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeTrackBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF07111F),
                    Color(0xFF0A1527),
                    Color(0xFF050813)
                )
            )
        )

        val horizon = size.height * 0.42f
        val trackTop = size.width * 0.16f
        val trackBottom = size.width * 0.82f
        val trackPath = Path().apply {
            moveTo(size.width * 0.5f - trackTop, horizon)
            lineTo(size.width * 0.5f + trackTop, horizon)
            lineTo(size.width * 0.5f + trackBottom, size.height)
            lineTo(size.width * 0.5f - trackBottom, size.height)
            close()
        }
        drawPath(
            trackPath,
            brush = Brush.verticalGradient(
                listOf(Color(0x88323C52), Color(0xEE070A12)),
                startY = horizon,
                endY = size.height
            )
        )

        val laneXs = listOf(-0.32f, -0.11f, 0.11f, 0.32f)
        laneXs.forEach { lane ->
            drawLine(
                color = Color(0xAA1CD7FF),
                start = Offset(size.width * (0.5f + lane * 0.35f), horizon),
                end = Offset(size.width * (0.5f + lane), size.height),
                strokeWidth = 2.dp.toPx()
            )
        }

        for (i in 0..9) {
            val t = i / 9f
            val y = horizon + (size.height - horizon) * t * t
            val half = trackTop + (trackBottom - trackTop) * t
            drawLine(
                color = Color(0x3329E6FF),
                start = Offset(size.width * 0.5f - half, y),
                end = Offset(size.width * 0.5f + half, y),
                strokeWidth = (1.5f + t * 3f).dp.toPx()
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x4435F3FF), Color.Transparent),
                center = Offset(size.width * 0.5f, horizon),
                radius = size.width * 0.55f
            ),
            center = Offset(size.width * 0.5f, horizon),
            radius = size.width * 0.55f
        )
    }
}

@Composable
private fun TopHud(
    coins: Int,
    shards: Int,
    tokens: Int,
    cash: Float,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyChip("COINS", coins.toString(), Color(0xFFFFC247))
            CurrencyChip("SHARDS", shards.toString(), Color(0xFFB996FF))
            CurrencyChip("TOKENS", tokens.toString(), Color(0xFF00E5FF))
            CurrencyChip("CASH", String.format("$%.2f", cash), Color(0xFF00FF66))
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xCC0B1020))
                .border(1.dp, Color(0x334A5B75), RoundedCornerShape(10.dp))
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("SET", color = Color(0xFFEAF2FF), fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CurrencyChip(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0B1020))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color(0xFF8EA3B4), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun FluxLogo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "P2E",
                style = TextStyle(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color(0xFF1CD7FF),
                        offset = Offset(0f, 0f),
                        blurRadius = 15f
                    )
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFC247), Color(0xFFFF9100))
                        )
                    )
                    .border(1.dp, Color(0xFFFFF2D0), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "FLUX RUNNER",
                    color = Color(0xFF070A12),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF1CD7FF), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun RunnerShowcase() {
    val transition = rememberInfiniteTransition(label = "core")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val radius = size.minDimension * 0.22f

            // Glowing backing aura
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x3A1CD7FF), Color.Transparent),
                    center = center,
                    radius = radius * 2.5f
                ),
                center = center,
                radius = radius * 2.5f
            )

            // Draw outer rotating cyber rings
            val ringStroke = Stroke(width = 3.dp.toPx())
            drawCircle(
                color = Color(0x771CD7FF),
                radius = radius * 1.5f * pulse,
                center = center,
                style = ringStroke
            )

            // Draw rotating orbit line
            rotate(rotation, center) {
                drawCircle(
                    color = Color(0x44FFE45C),
                    radius = radius * 1.8f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                )
                // Draw 4 elemental node anchors along the orbit
                val angles = listOf(0f, 90f, 180f, 270f)
                val colors = listOf(Color(0xFFFF5E36), Color(0xFF35F3FF), Color(0xFFFFE45C), Color(0xFF00FF8A))
                angles.forEachIndexed { index, angle ->
                    val rad = Math.toRadians(angle.toDouble())
                    val ox = center.x + cos(rad).toFloat() * radius * 1.8f
                    val oy = center.y + sin(rad).toFloat() * radius * 1.8f
                    drawCircle(
                        color = colors[index],
                        radius = 8.dp.toPx(),
                        center = Offset(ox, oy)
                    )
                }
            }

            // Draw core reactor sphere
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, Color(0xFF1CD7FF), Color(0xFF07111F)),
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                    radius = radius
                ),
                center = center,
                radius = radius
            )
            drawCircle(Color(0xFF050813), radius = radius * 0.45f, center = center)
            drawCircle(Color(0xFF1CD7FF), radius = radius * 0.25f, center = center)
        }

        // Display elemental symbols orbiting the core using Canvas
        Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
            val orbitRadius = 86.dp
            val radians = Math.toRadians(rotation.toDouble())

            // 4 elemental colored dots on the orbit path (no emojis)
            val elements = listOf(Element.FIRE, Element.ICE, Element.ELECTRIC, Element.TOXIC)
            elements.forEachIndexed { index, element ->
                val angle = radians + index * (Math.PI / 2.0)
                val x = (cos(angle) * orbitRadius.value).dp
                val y = (sin(angle) * orbitRadius.value).dp

                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(element.primaryColor).copy(alpha = 0.9f), Color(element.primaryColor).copy(alpha = 0.3f))
                            )
                        )
                        .border(1.5.dp, Color(element.primaryColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ElementMark(element = element, modifier = Modifier.size(18.dp))
                }
            }
        }

        Text(
            text = "ACTIVE REACTOR CORE",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xD00B1020))
                .border(1.dp, Color(0x334A5B75), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            color = Color(0xFFEAF2FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun QuickStats(highScore: Int, maxDistance: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("BEST", highScore.toString(), Modifier.weight(1f))
        StatCard("DISTANCE", "${maxDistance}m", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD00B1020))
            .border(1.dp, Color(0x2235F3FF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = Color(0xFF8EA3B4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFD76B), Color(0xFFFFA51F))
                )
            )
            .border(2.dp, Color(0xFFFFF1BC), RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PLAY",
            color = Color(0xFF11131A),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DockButton(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xDD0B1020))
            .border(1.dp, Color(0x334A5B75), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Color(0xFF8EA3B4), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EarnCashButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF00C853), Color(0xFF007B2E))
                )
            )
            .border(1.5.dp, Color(0xFF69F0AE), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text("EARN", color = Color(0xFFB9F6CA), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("REAL CASH", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}
