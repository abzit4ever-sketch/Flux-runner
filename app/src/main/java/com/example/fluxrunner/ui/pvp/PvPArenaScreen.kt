package com.example.fluxrunner.ui.pvp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluxrunner.R
import com.example.fluxrunner.logic.SaveManager
import com.example.fluxrunner.network.PvpClient
import com.example.fluxrunner.network.PvpMatchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext

@Composable
fun PvPArenaScreen(
    saveManager: SaveManager,
    onBackClick: () -> Unit,
    onStartMatch: (matchId: String, playerId: String, seed: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val serverUrl = remember { context.getString(R.string.pvp_server_url).trimEnd('/') }
    val client = remember(serverUrl) { PvpClient(serverUrl) }
    val scope = rememberCoroutineScope()
    val playerId = remember { "android-${System.currentTimeMillis()}" }
    val playerName = remember { "Runner-${(100..999).random()}" }
    val tokens = saveManager.getTokens()

    var match by remember { mutableStateOf<PvpMatchState?>(null) }
    var isQueueing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("Server: $serverUrl") }

    LaunchedEffect(match?.matchId, match?.status) {
        val current = match ?: return@LaunchedEffect
        if (current.status != "waiting") return@LaunchedEffect

        while (true) {
            delay(1000)
            val updated = runCatching {
                withContext(Dispatchers.IO) { client.getState(current.matchId, current.playerId) }
            }.getOrElse {
                notice = it.message ?: "Could not reach PvP server."
                return@LaunchedEffect
            }
            match = updated
            if (updated.hasOpponent && updated.status == "active") {
                onStartMatch(updated.matchId, updated.playerId, updated.seed)
                return@LaunchedEffect
            }
        }
    }

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF07111F), Color(0xFF090A15), Color(0xFF050813))))
            .safeDrawingPadding()
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    "PVP ARENA",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(Color(0xFF1CD7FF), Offset.Zero, 14f)
                    )
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOKENS", color = Color(0xFF8EA3B4), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(tokens.toString(), color = Color(0xFF00E5FF), fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }

            ArenaHeroCard()

            MatchmakingCard(
                isQueueing = isQueueing,
                match = match,
                onQueue = {
                    isQueueing = true
                    notice = "Finding another player..."
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) { client.queue(playerId, playerName) }
                        }
                        isQueueing = false
                        result.onSuccess { queuedMatch ->
                            match = queuedMatch
                            notice = if (queuedMatch.status == "active" && queuedMatch.hasOpponent) {
                                "Opponent found. Starting duel..."
                            } else {
                                "Waiting for another player to join."
                            }
                            if (queuedMatch.status == "active" && queuedMatch.hasOpponent) {
                                onStartMatch(queuedMatch.matchId, queuedMatch.playerId, queuedMatch.seed)
                            }
                        }.onFailure {
                            notice = it.message ?: "Could not reach PvP server."
                        }
                    }
                }
            )

            HowItWorksCard()

            Text(
                notice,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD0B1020))
                    .border(1.dp, Color(0x444A5B75), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                color = Color(0xFFEAF2FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ArenaHeroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0C1F2C), Color(0xFF111827))))
            .border(1.dp, Color(0x5535F3FF), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Text("ONLINE SKILL DUEL", color = Color(0xFF8EA3B4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Race the same duel seed against another player.", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "The server matches two players, syncs live scores, and decides the winner by score, then distance.",
            color = Color(0xFF9AAABD),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MatchmakingCard(
    isQueueing: Boolean,
    match: PvpMatchState?,
    onQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0B1020))
            .border(1.dp, Color(0x5500E5FF), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0x2200E5FF))
                    .border(2.dp, Color(0xFF00E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("VS", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("FREE ENTRY", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text("MATCHMAKING DUEL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(match?.let { "Match ${it.matchId.takeLast(6)} - ${it.status.uppercase()}" } ?: "Ready to find a rival", color = Color(0xFF8EA3B4), fontSize = 11.sp)
            }
        }

        Button(
            onClick = onQueue,
            enabled = !isQueueing,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), disabledContainerColor = Color(0x5535F3FF))
        ) {
            Text(if (isQueueing) "QUEUEING..." else "FIND PLAYER", color = Color(0xFF07111F), fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xAA07111F))
            .border(1.dp, Color(0x224A5B75), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("HOW PVP WORKS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        listOf(
            "Both players get one timed run.",
            "Live score and distance are synced to the server.",
            "Highest score wins. Distance breaks ties.",
            "Winner gets a free-entry token reward."
        ).forEach { item ->
            Text(item, color = Color(0xFF9AAABD), fontSize = 11.sp)
        }
    }
}
