package com.example.fluxrunner.ui.missions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluxrunner.logic.MissionManager
import com.example.fluxrunner.model.Mission

@Composable
fun MissionsScreen(
    missionManager: MissionManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Force reload on entry
    LaunchedEffect(Unit) {
        missionManager.initMissions()
    }

    // Local mutable state list of missions to trigger recomposition on claim
    val missionsList = remember { mutableStateListOf<Mission>().apply { addAll(missionManager.activeMissions) } }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090A15), Color(0xFF0B1420), Color(0xFF05080E))
                )
            )
            .padding(16.dp)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("<", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Text(
                    text = "DAILY MISSIONS",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(Color(0xFF00FF66), Offset.Zero, 12f)
                    )
                )

                // Dummy spacer for symmetry
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LIST OF MISSIONS
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(missionsList) { mission ->
                    MissionItemRow(
                        mission = mission,
                        onClaimClick = {
                            if (missionManager.claimReward(mission)) {
                                // Reload to force UI updates
                                missionsList.clear()
                                missionsList.addAll(missionManager.activeMissions)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MissionItemRow(
    mission: Mission,
    onClaimClick: () -> Unit
) {
    val progressPercent = (mission.currentValue.toFloat() / mission.targetValue).coerceIn(0f, 1f)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x12FFFFFF))
            .border(
                1.dp,
                if (mission.completed && !mission.claimed) Color(0x6600FF66) else Color(0x1AFFFFFF),
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mission.description,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "COINS +${mission.coinReward}",
                color = Color(0xFFFFD700),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress bar and numeric tracking
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (mission.completed) Color(0xFF00FF66) else Color(0xFF00E5FF),
                    trackColor = Color(0x1AFFFFFF)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "${mission.currentValue} / ${mission.targetValue}",
                    color = Color(0xFF90A4AE),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Claim Status Actions
            if (mission.claimed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1A00FF66))
                        .border(1.dp, Color(0xFF00FF66), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("CLAIMED", color = Color(0xFF00FF66), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            } else if (mission.completed) {
                Button(
                    onClick = onClaimClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("CLAIM", color = Color(0xFF05080E), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0DFFFFFF))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("IN PROGRESS", color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
