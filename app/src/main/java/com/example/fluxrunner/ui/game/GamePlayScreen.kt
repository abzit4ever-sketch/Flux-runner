package com.example.fluxrunner.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fluxrunner.R
import com.example.fluxrunner.engine.GameView
import com.example.fluxrunner.logic.GameManager
import com.example.fluxrunner.logic.GameState
import com.example.fluxrunner.model.Element
import com.example.fluxrunner.network.PvpClient
import com.example.fluxrunner.network.PvpMatchState
import com.example.fluxrunner.ui.common.ElementMark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun GamePlayScreen(
    gameManager: GameManager,
    onBackClick: () -> Unit,
    pvpMatchId: String? = null,
    pvpPlayerId: String? = null,
    pvpSeed: Int? = null,
    modifier: Modifier = Modifier
) {
    // HUD State holders
    var score by remember { mutableStateOf(0) }
    var distance by remember { mutableStateOf(0f) }
    var coins by remember { mutableStateOf(0) }
    var shards by remember { mutableStateOf(0) }
    var totalTokens by remember { mutableStateOf(0) }
    var totalCash by remember { mutableStateOf(0f) }
    var runTimer by remember { mutableStateOf(0f) }
    var combo by remember { mutableStateOf(0) }
    var activeElement by remember { mutableStateOf(Element.FIRE) }
    var overchargeActive by remember { mutableStateOf(false) }
    var skillCallout by remember { mutableStateOf("") }
    var fluxSurgeActive by remember { mutableStateOf(false) }
    var orbEvolutionLevel by remember { mutableStateOf(1) }
    var luckyRun by remember { mutableStateOf(false) }
    var shieldTimer by remember { mutableStateOf(0f) }
    var magnetTimer by remember { mutableStateOf(0f) }
    var invisibilityTimer by remember { mutableStateOf(0f) }
    var hasRevivedThisRun by remember { mutableStateOf(false) }
    var isShowingAd by remember { mutableStateOf(false) }
    var adCountdown by remember { mutableStateOf(5) }
    var onAdCompleted by remember { mutableStateOf<(() -> Unit)?>(null) }
    var gameState by remember { mutableStateOf(GameState.MENU) }
    val context = LocalContext.current
    val pvpServerUrl = remember { context.getString(R.string.pvp_server_url).trimEnd('/') }
    val pvpClient = remember(pvpServerUrl) { PvpClient(pvpServerUrl) }
    var pvpState by remember { mutableStateOf<PvpMatchState?>(null) }
    var pvpNotice by remember { mutableStateOf("") }
    var pvpRewardClaimed by remember { mutableStateOf(false) }
    val isPvpMode = pvpMatchId != null && pvpPlayerId != null

    // Start game run when entering this screen
    LaunchedEffect(gameManager, pvpMatchId, pvpPlayerId, pvpSeed) {
        hasRevivedThisRun = false
        gameManager.startRun(pvpSeed)
        
        // Polling thread for updating HUD stats from game loop
        while (true) {
            score = gameManager.score
            distance = gameManager.distanceTraveled
            coins = gameManager.coinsCollected
            shards = gameManager.shardsCollected
            totalTokens = gameManager.saveManager.getTokens()
            totalCash = gameManager.saveManager.getRealCash()
            runTimer = gameManager.runTimer
            combo = gameManager.combo
            activeElement = gameManager.player.activeElement
            overchargeActive = gameManager.isOverchargeActive
            skillCallout = gameManager.skillCallout
            fluxSurgeActive = gameManager.isFluxSurgeActive
            orbEvolutionLevel = gameManager.orbEvolutionLevel
            luckyRun = gameManager.isLuckyRun
            shieldTimer = gameManager.shieldTimer
            magnetTimer = gameManager.magnetTimer
            invisibilityTimer = gameManager.invisibilityTimer
            gameState = gameManager.gameState
            delay(100)
        }
    }

    LaunchedEffect(pvpMatchId, pvpPlayerId) {
        val matchId = pvpMatchId ?: return@LaunchedEffect
        val playerId = pvpPlayerId ?: return@LaunchedEffect

        while (true) {
            delay(1000)
            val finished = gameState == GameState.GAME_OVER
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    pvpClient.submitState(
                        matchId = matchId,
                        playerId = playerId,
                        score = score,
                        distance = distance,
                        alive = !finished,
                        finished = finished
                    )
                }
            }
            result.onSuccess { state ->
                pvpState = state
                if (state.isFinished) {
                    val didWin = state.winnerId == playerId
                    pvpNotice = if (didWin) "PVP WIN +${state.tokenReward} TOKENS" else "PVP DEFEAT"
                    if (didWin && !pvpRewardClaimed) {
                        gameManager.saveManager.addTokens(state.tokenReward)
                        pvpRewardClaimed = true
                    }
                    return@LaunchedEffect
                }
            }.onFailure {
                pvpNotice = it.message ?: "PvP sync failed"
            }
        }
    }

    LaunchedEffect(isShowingAd) {
        if (isShowingAd) {
            while (adCountdown > 0) {
                delay(1000)
                adCountdown--
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF090A15))) {
        // 1. Render custom GameView (SurfaceView drawing loop)
        AndroidView(
            factory = { ctx ->
                GameView(ctx).apply {
                    this.gameManager = gameManager
                    this.startLoop()
                }
            },
            update = { view ->
                // Start or pause loop depending on active lifecycle state
                if (gameState == GameState.PLAYING) {
                    view.startLoop()
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { view ->
                view.stopLoop()
                gameManager.audioManager.stopMusic()
            }
        )

        // 2. Play HUD Overlay (Top & Bottom controls)
        if (gameState == GameState.PLAYING) {
            GameHUD(
                score = score,
                distance = distance,
                coins = coins,
                shards = shards,
                tokens = totalTokens,
                cash = totalCash,
                runTimer = runTimer,
                combo = combo,
                activeElement = activeElement,
                overchargeActive = overchargeActive,
                skillCallout = skillCallout,
                fluxSurgeActive = fluxSurgeActive,
                orbEvolutionLevel = orbEvolutionLevel,
                luckyRun = luckyRun,
                shieldTimer = shieldTimer,
                magnetTimer = magnetTimer,
                invisibilityTimer = invisibilityTimer,
                onPauseClick = { gameManager.pauseGame() }
            )
        }

        if (isPvpMode) {
            PvPOverlay(
                match = pvpState,
                notice = pvpNotice,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
            )
        }

        // 3. Pause Overlay Screen
        AnimatedVisibility(
            visible = gameState == GameState.PAUSED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PauseMenuOverlay(
                onResume = { gameManager.resumeGame() },
                onQuit = {
                    gameManager.audioManager.stopMusic()
                    onBackClick()
                }
            )
        }

        // 4. Game Over Overlay Screen
        AnimatedVisibility(
            visible = gameState == GameState.GAME_OVER,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut()
        ) {
            GameOverOverlay(
                score = score,
                distance = distance,
                coins = coins,
                shards = shards,
                hasRevived = hasRevivedThisRun,
                isPvpMode = isPvpMode,
                onReviveClick = {
                    onAdCompleted = {
                        gameManager.reviveRun()
                        hasRevivedThisRun = true
                    }
                    adCountdown = 5
                    isShowingAd = true
                },
                onRestart = {
                    hasRevivedThisRun = false
                    gameManager.startRun(pvpSeed)
                },
                onMainMenu = {
                    gameManager.audioManager.stopMusic()
                    onBackClick()
                }
            )
        }

        // 5. Simulated Video Ad Overlay
        if (isShowingAd) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF9050813))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F1424))
                        .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "SPONSORED AD",
                        color = Color(0xFF8EA3B4),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(width = 200.dp, height = 120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E2638), Color(0xFF0E1420))
                                )
                            )
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Flux Merge logo (no emoji)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF5500CC))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("FX", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "FLUX MERGE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Play matching elements!",
                                color = Color(0xFF8EA3B4),
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    if (adCountdown > 0) {
                        Text(
                            text = "Ad ends in ${adCountdown}s...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Button(
                            onClick = {
                                isShowingAd = false
                                onAdCompleted?.invoke()
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66))
                        ) {
                            Text("CLOSE AD", color = Color(0xFF070A12), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PowerUpHUDIndicator(name: String, timeLeft: Float, maxTime: Float, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x77000000))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored dot indicator instead of emoji
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(5.dp)
                    .background(Color(0x33FFFFFF), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width((70 * (timeLeft / maxTime)).coerceIn(0f, 70f).dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
private fun PvPOverlay(
    match: PvpMatchState?,
    notice: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(250.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xAA050813))
            .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ONLINE DUEL", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Black)
        val opponent = match?.opponent
        if (opponent == null) {
            Text("SYNCING OPPONENT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(opponent.displayName.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${opponent.score} PTS", color = Color(0xFF9AAABD), fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${opponent.distance.toInt()}m", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(if (opponent.alive) "RUNNING" else "FINISHED", color = Color(0xFF9AAABD), fontSize = 8.sp)
                }
            }
        }
        if (notice.isNotBlank()) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(notice, color = Color(0xFF00FF66), fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun WalletLine(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, color = Color(0xFF9AAABD), fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
        Text(value, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun GameHUD(
    score: Int,
    distance: Float,
    coins: Int,
    shards: Int,
    tokens: Int,
    cash: Float,
    runTimer: Float,
    combo: Int,
    activeElement: Element,
    overchargeActive: Boolean,
    skillCallout: String,
    fluxSurgeActive: Boolean,
    orbEvolutionLevel: Int,
    luckyRun: Boolean,
    shieldTimer: Float,
    magnetTimer: Float,
    invisibilityTimer: Float,
    onPauseClick: () -> Unit
) {
    val scale = 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .safeDrawingPadding()
    ) {
        // TOP BAR: Pause, Score, Coins
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x55000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .shadow(4.dp, CircleShape)
                    .align(Alignment.CenterVertically)
                    .clickable { onPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("II", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
            }

            // Central Score Board
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%06d", score),
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color(0xFF00E5FF),
                            offset = Offset(0f, 0f),
                            blurRadius = 15f
                        )
                    )
                )
                Text(
                    text = "${distance.toInt()}m",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB0BEC5)
                )
            }

            Column(
                modifier = Modifier
                    .width(118.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x55000000))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                WalletLine("COINS", coins.toString(), Color(0xFFFFD700))
                WalletLine("SHARDS", shards.toString(), Color(0xFFB388FF))
                WalletLine("TOKENS", tokens.toString(), Color(0xFF00E5FF))
                WalletLine("CASH", String.format("$%.2f", cash), Color(0xFF00FF66))
            }
        }

        if (shards > 0) {
            Text(
                text = "SHARDS +$shards",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 58.dp),
                color = Color(0xFFB388FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (skillCallout.isNotBlank()) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-110).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xAA07101A))
                    .border(1.dp, Color(0x5535F3FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                text = skillCallout,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = when (skillCallout) {
                    "GOLDEN GATE", "MAGNET ACTIVE" -> Color(0xFFFFD700)
                    "SHIELD ACTIVE" -> Color(0xFF35F3FF)
                    "PHASE ACTIVE" -> Color(0xFFB388FF)
                    else -> Color.White
                }
            )
        }

        if (runTimer in 0.4f..6.0f) {
            StarterCoachOverlay(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 92.dp),
                activeElement = activeElement
            )
        }

        // Lucky run indicator only (Pace/Evo removed)
        if (luckyRun) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LUCKY RUN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700)
                )
            }
        }

        // ACTIVE POWERUP TIMERS OVERLAY (Middle-Left)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (shieldTimer > 0f) {
                PowerUpHUDIndicator(name = "SHIELD", color = Color(0xFF35F3FF), timeLeft = shieldTimer, maxTime = 8f)
            }
            if (magnetTimer > 0f) {
                PowerUpHUDIndicator(name = "MAGNET", color = Color(0xFFFFD700), timeLeft = magnetTimer, maxTime = 10f)
            }
            if (invisibilityTimer > 0f) {
                PowerUpHUDIndicator(name = "PHASE", color = Color(0xFFB388FF), timeLeft = invisibilityTimer, maxTime = 8f)
            }
        }

        // BOTTOM LEFT: Active Element HUD Indicator
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x66000000))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ELEMENT", fontSize = 10.sp, color = Color(0xFFECEFF1), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(activeElement.primaryColor), Color.Transparent)
                        )
                    )
                    .border(2.dp, Color(activeElement.primaryColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ElementMark(
                    element = activeElement,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activeElement.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(activeElement.primaryColor)
            )
        }

        // BOTTOM RIGHT: Combo Indicator
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = 0.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (combo >= 2) {
                Box(
                    modifier = Modifier
                        .size((64f * scale).dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                        .border(
                            2.dp,
                            Color(0xFF00E5FF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${combo}x",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            "COMBO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterCoachOverlay(modifier: Modifier, activeElement: Element) {
    val transition = rememberInfiniteTransition(label = "starterCoach")
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xAA050813))
            .border(1.dp, Color(0x6635F3FF), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size((42f * pulse).dp)
                .clip(CircleShape)
                .background(Color(activeElement.primaryColor).copy(alpha = 0.25f))
                .border(2.dp, Color(activeElement.primaryColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ElementMark(element = activeElement, modifier = Modifier.size(24.dp))
        }
        Column {
            Text("TAP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("SWITCH CORE", color = Color(0xFF9AAABD), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(34.dp)
                .background(Color(0x334A5B75))
        )
        Column {
            Text("SWIPE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("CHANGE LANE", color = Color(0xFF9AAABD), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PauseMenuOverlay(
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF140D25))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "PAUSED",
                style = TextStyle(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    shadow = Shadow(Color(0xFF00E5FF), Offset.Zero, 20f)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("RESUME RUN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF140D25))
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onQuit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Text("QUIT TO MENU", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    score: Int,
    distance: Float,
    coins: Int,
    shards: Int,
    hasRevived: Boolean,
    isPvpMode: Boolean = false,
    onReviveClick: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD90C020B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF17040C))
                .border(2.dp, Color(0xFFFF0055), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "RUN ENDED",
                style = TextStyle(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    shadow = Shadow(Color(0xFFFF0055), Offset.Zero, 25f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Score Card info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1F000000))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("FINAL SCORE:", color = Color(0xFFECEFF1), fontSize = 12.sp)
                    Text("$score", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DISTANCE:", color = Color(0xFFECEFF1), fontSize = 12.sp)
                    Text("${distance.toInt()}m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("COINS EARNED:", color = Color(0xFFECEFF1), fontSize = 12.sp)
                    Text("+$coins", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!hasRevived && !isPvpMode) {
                Button(
                    onClick = onReviveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("WATCH AD TO REVIVE", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17040C))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Play again button
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = Color(0xFFFF0055)),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055))
            ) {
                Text("RESTART RUN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onMainMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Text("MAIN MENU", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
