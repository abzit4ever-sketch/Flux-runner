package com.example.fluxrunner.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.fluxrunner.logic.SaveManager
import com.example.fluxrunner.media.AudioManager

@Composable
fun SettingsScreen(
    saveManager: SaveManager,
    audioManager: AudioManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSoundEnabled by remember { mutableStateOf(saveManager.isSoundEnabled()) }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090A15), Color(0xFF140B1A), Color(0xFF07040A))
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
                    text = "SETTINGS",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(Color(0xFFFFCC00), Offset.Zero, 12f)
                    )
                )

                // Dummy spacer for symmetry
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AUDIO CONTROLS CARD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x12FFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SFX & Music Synth", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Toggle game music and synth audio", color = Color(0xFF90A4AE), fontSize = 12.sp)
                }
                
                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = { checked ->
                        saveManager.setSoundEnabled(checked)
                        isSoundEnabled = checked
                        if (checked) {
                            audioManager.startMusic()
                        } else {
                            audioManager.stopMusic()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00E5FF),
                        checkedTrackColor = Color(0x6600E5FF),
                        uncheckedThumbColor = Color(0xFF90A4AE),
                        uncheckedTrackColor = Color(0x1AFFFFFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // HOW TO PLAY INSTRUCTIONS CARD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x12FFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text("HOW TO PLAY", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                    InstructionStep("POWER TAP SCREEN", "Cycles the orb element state: FIRE to ICE to ELECTRIC to TOXIC.")
                Spacer(modifier = Modifier.height(10.dp))
                InstructionStep("SWIPE LEFT/RIGHT", "Moves the orb between the 3 lanes to dodge barriers or select paths.")
                Spacer(modifier = Modifier.height(10.dp))
                InstructionStep("MATCH GATES", "You must match the element color of incoming gates. Wrong match is instant crash!")
                Spacer(modifier = Modifier.height(10.dp))
                InstructionStep("PERFECT SWITCHES", "Switch at the last moment before a gate for bonus score, combo boosts, and rare shard drops.")
            }

            Spacer(modifier = Modifier.weight(1f))

            // CREDITS FOOTER
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PLAY-TO-EARN FLUX RUNNER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0x66FFFFFF)
                )
                Text(
                    text = "v1.0.0 - Powered by Antigravity Engine",
                    fontSize = 10.sp,
                    color = Color(0x40FFFFFF)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun InstructionStep(title: String, desc: String) {
    Column {
        Text(title, color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(desc, color = Color(0xFFECEFF1), fontSize = 11.sp)
    }
}
