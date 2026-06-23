package com.example.fluxrunner.ui.earn

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluxrunner.logic.SaveManager
import kotlinx.coroutines.delay

// Exchange rate constants
private val COIN_BUNDLES = listOf(
    Triple(100, 10, "STARTER"),
    Triple(500, 55, "VALUE"),
    Triple(1000, 120, "ELITE")
)
private val TOKEN_BUNDLES = listOf(
    Triple(100, 1.00f, "BASIC"),
    Triple(450, 5.00f, "SILVER"),
    Triple(800, 10.00f, "GOLD")
)
private const val AD_REWARD_TOKENS = 15

@Composable
fun EarnCashScreen(
    saveManager: SaveManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coins by remember { mutableStateOf(saveManager.getCoins()) }
    var tokens by remember { mutableStateOf(saveManager.getTokens()) }
    var cash by remember { mutableStateOf(saveManager.getRealCash()) }

    // Ad / processing states
    var isShowingAd by remember { mutableStateOf(false) }
    var adCountdown by remember { mutableStateOf(5) }
    var onAdCompleted by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Withdraw dialog states
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("PayPal") }
    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableStateOf(0f) }
    var showSuccess by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // Snack-bar feedback
    var feedbackMsg by remember { mutableStateOf("") }

    // Ad countdown coroutine
    LaunchedEffect(isShowingAd) {
        if (isShowingAd) {
            adCountdown = 5
            while (adCountdown > 0) {
                delay(1000L)
                adCountdown--
            }
        }
    }

    // Processing animation
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            processingProgress = 0f
            repeat(30) {
                delay(80L)
                processingProgress += 1f / 30f
            }
            delay(200L)
            isProcessing = false
            showSuccess = true
        }
    }

    // Feedback auto-clear
    LaunchedEffect(feedbackMsg) {
        if (feedbackMsg.isNotBlank()) {
            delay(2500L)
            feedbackMsg = ""
        }
    }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080E1A), Color(0xFF0A1A12), Color(0xFF060D04))
                )
            )
            .safeDrawingPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("<", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = "EARN REAL CASH",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            shadow = Shadow(Color(0xFF00FF66), Offset.Zero, 14f)
                        )
                    )
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            // Balance chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BalanceChip("COINS", coins.toString(), Color(0xFFFFC247), Modifier.weight(1f))
                    BalanceChip("TOKENS", tokens.toString(), Color(0xFF00E5FF), Modifier.weight(1f))
                    BalanceChip("CASH", String.format("$%.2f", cash), Color(0xFF00FF66), Modifier.weight(1f))
                }
            }

            // Section: coins to tokens
            item { SectionHeader("COINS TO TOKENS") }

            items(COIN_BUNDLES.size) { i ->
                val (cost, reward, tier) = COIN_BUNDLES[i]
                ExchangeCard(
                    fromLabel = "$cost COINS",
                    toLabel = "$reward TOKENS",
                    tier = tier,
                    fromColor = Color(0xFFFFC247),
                    toColor = Color(0xFF00E5FF),
                    onExchange = {
                        if (saveManager.spendCoins(cost)) {
                            saveManager.addTokens(reward)
                            coins = saveManager.getCoins()
                            tokens = saveManager.getTokens()
                            feedbackMsg = "+$reward tokens received!"
                        } else {
                            feedbackMsg = "Not enough coins."
                        }
                    }
                )
            }

            // Section: tokens to cash
            item { SectionHeader("TOKENS TO CASH") }

            items(TOKEN_BUNDLES.size) { i ->
                val (cost, reward, tier) = TOKEN_BUNDLES[i]
                ExchangeCard(
                    fromLabel = "$cost TOKENS",
                    toLabel = String.format("$%.2f USD", reward),
                    tier = tier,
                    fromColor = Color(0xFF00E5FF),
                    toColor = Color(0xFF00FF66),
                    onExchange = {
                        if (saveManager.spendTokens(cost)) {
                            saveManager.addRealCash(reward)
                            tokens = saveManager.getTokens()
                            cash = saveManager.getRealCash()
                            feedbackMsg = String.format("+$%.2f added to balance!", reward)
                        } else {
                            feedbackMsg = "Not enough tokens."
                        }
                    }
                )
            }

            // Section: watch ad
            item { SectionHeader("WATCH AD FOR TOKENS") }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0C1F2C), Color(0xFF0C2918))
                            )
                        )
                        .border(1.dp, Color(0x5500E5FF), RoundedCornerShape(16.dp))
                        .clickable {
                            onAdCompleted = {
                                saveManager.addTokens(AD_REWARD_TOKENS)
                                tokens = saveManager.getTokens()
                                feedbackMsg = "+$AD_REWARD_TOKENS tokens for watching an ad!"
                            }
                            isShowingAd = true
                        }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "WATCH AD",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                "Earn +$AD_REWARD_TOKENS Tokens for free",
                                fontSize = 11.sp,
                                color = Color(0xFF8EA3B4)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF00E5FF))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("WATCH", color = Color(0xFF07111F), fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Spacer at bottom
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        // Feedback snackbar
        if (feedbackMsg.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE002B14))
                    .border(1.dp, Color(0xFF00FF66), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(feedbackMsg, color = Color(0xFF00FF66), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Simulated ad overlay
        if (isShowingAd) {
            SimulatedAdOverlay(
                countdown = adCountdown,
                onClose = {
                    isShowingAd = false
                    onAdCompleted?.invoke()
                }
            )
        }

        // Withdraw dialog
        if (showWithdrawDialog) {
            WithdrawDialog(
                cashBalance = cash,
                selectedMethod = selectedMethod,
                onMethodChange = { selectedMethod = it },
                withdrawAmount = withdrawAmount,
                onAmountChange = { withdrawAmount = it },
                onConfirm = {
                    val amt = withdrawAmount.toFloatOrNull()
                    if (amt != null && amt > 0 && amt <= cash) {
                        showWithdrawDialog = false
                        isProcessing = true
                        successMessage = String.format("$%.2f sent via %s!", amt, selectedMethod)
                        saveManager.spendRealCash(amt)
                        cash = saveManager.getRealCash()
                    } else {
                        feedbackMsg = "Invalid amount."
                    }
                },
                onDismiss = { showWithdrawDialog = false }
            )
        }

        // Processing / success overlays
        if (isProcessing) {
            ProcessingOverlay(progress = processingProgress, method = selectedMethod)
        }
        if (showSuccess) {
            SuccessOverlay(message = successMessage, onDismiss = { showSuccess = false })
        }
    }
}

// Composable components

@Composable
private fun BalanceChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0B1020))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFF8EA3B4), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.height(1.dp).weight(1f).background(Color(0x2200FF66)))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, color = Color(0xFF69F0AE), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.height(1.dp).weight(1f).background(Color(0x2200FF66)))
    }
}

@Composable
private fun ExchangeCard(
    fromLabel: String,
    toLabel: String,
    tier: String,
    fromColor: Color,
    toColor: Color,
    onExchange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC0B1020))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(tier, color = Color(0xFF5A6A7A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fromLabel, color = fromColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("TO", color = Color(0xFF5A6A7A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(toLabel, color = toColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1CD7FF), Color(0xFF0088AA)))
                )
                .clickable { onExchange() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("SWAP", color = Color(0xFF07111F), fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun WithdrawCard(cashBalance: Float, onWithdraw: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1F10), Color(0xFF061208))
                )
            )
            .border(1.5.dp, Color(0xFF00FF66).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text("AVAILABLE BALANCE", color = Color(0xFF8EA3B4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            String.format("$%.2f USD", cashBalance),
            color = Color(0xFF00FF66),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Minimum withdrawal: \$1.00  |  Via PayPal or Google Pay",
            color = Color(0xFF5A6A7A),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // PayPal button
            MethodButton("PayPal", Color(0xFF003087), Color(0xFF009CDE), Modifier.weight(1f), onWithdraw)
            // Google Pay button
            MethodButton("Google Pay", Color(0xFF1A73E8), Color(0xFF34A853), Modifier.weight(1f), onWithdraw)
        }
    }
}

@Composable
private fun MethodButton(
    name: String,
    fromColor: Color,
    toColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(listOf(fromColor, toColor)))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SimulatedAdOverlay(countdown: Int, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF4050813))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F1424))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SPONSORED", color = Color(0xFF5A6A7A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))
            // Ad creative, branded box
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E2638), Color(0xFF0C1020))
                        )
                    )
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF5500CC)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FX", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("FLUX MERGE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Match. Evolve. Win.", color = Color(0xFF8EA3B4), fontSize = 9.sp)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            if (countdown > 0) {
                Text(
                    "Ad ends in ${countdown}s",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66))
                ) {
                    Text("CLOSE AD", color = Color(0xFF070A12), fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun WithdrawDialog(
    cashBalance: Float,
    selectedMethod: String,
    onMethodChange: (String) -> Unit,
    withdrawAmount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB000000))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0C1120))
                .border(2.dp, Color(0xFF00FF66).copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Text("WITHDRAW CASH", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Available: ${String.format("$%.2f", cashBalance)}",
                color = Color(0xFF00FF66),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Method selector
            Text("PAYMENT METHOD", color = Color(0xFF8EA3B4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("PayPal", "Google Pay").forEach { method ->
                    val selected = method == selectedMethod
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF00FF66) else Color(0xFF131E2A))
                            .border(1.dp, if (selected) Color(0xFF00FF66) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .clickable { onMethodChange(method) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            method,
                            color = if (selected) Color(0xFF0A1A0E) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount input
            Text("AMOUNT (USD)", color = Color(0xFF8EA3B4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = withdrawAmount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 5.00", color = Color(0xFF5A6A7A)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF66),
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00FF66)
                ),
                singleLine = true
            )

            // SSL indicator
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF66))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "256-bit SSL secured connection",
                    color = Color(0xFF5A6A7A),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CANCEL", color = Color.White, fontSize = 13.sp)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66))
                ) {
                    Text("WITHDRAW", color = Color(0xFF0A1A0E), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ProcessingOverlay(progress: Float, method: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050813))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0C1120))
                .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CONNECTING TO $method", color = Color(0xFF8EA3B4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x2200FF66))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00FF66), Color(0xFF1CD7FF))
                            ),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            val stepLabels = listOf(
                "Verifying identity...",
                "Checking balance...",
                "Establishing secure connection...",
                "Processing transfer..."
            )
            val stepIdx = (progress * stepLabels.size).toInt().coerceIn(0, stepLabels.size - 1)
            Text(stepLabels[stepIdx], color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SuccessOverlay(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050813))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF081A0E))
                .border(2.dp, Color(0xFF00FF66), RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success checkmark via canvas circle + text
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00FF66)),
                contentAlignment = Alignment.Center
            ) {
                Text("OK", color = Color(0xFF081A0E), fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "TRANSFER SUCCESSFUL",
                color = Color(0xFF00FF66),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Funds typically arrive within 1-3 business days.",
                color = Color(0xFF5A6A7A),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66))
            ) {
                Text("DONE", color = Color(0xFF081A0E), fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}
