package com.example.fluxrunner.ui.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluxrunner.logic.GameManager
import com.example.fluxrunner.logic.SaveManager
import com.example.fluxrunner.model.Skin

@Composable
fun ShopScreen(
    saveManager: SaveManager,
    gameManager: GameManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var playerCoins by remember { mutableStateOf(saveManager.getCoins()) }
    var equippedSkinId by remember { mutableStateOf(saveManager.getActiveSkin()) }
    // Track skin unlock states locally to force recompositions
    val unlockedSkins = remember { mutableStateMapOf<String, Boolean>() }
    
    // Initialise unlock tracking
    LaunchedEffect(Unit) {
        for (skin in Skin.values()) {
            unlockedSkins[skin.id] = saveManager.isSkinUnlocked(skin.id)
        }
    }
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090A15), Color(0xFF0C071D), Color(0xFF060309))
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
                    text = "SKIN VAULT",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(Color(0xFF00E5FF), Offset.Zero, 12f)
                    )
                )

                // Coins counter
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("COINS", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$playerCoins",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(Skin.values()) { skin ->
                    val isUnlocked = unlockedSkins[skin.id] == true
                    val isEquipped = equippedSkinId == skin.id

                    SkinItemRow(
                        skin = skin,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        onActionClick = {
                            if (isUnlocked) {
                                saveManager.setActiveSkin(skin.id)
                                gameManager.player.activeSkin = skin
                                equippedSkinId = skin.id
                            } else if (saveManager.spendCoins(skin.cost)) {
                                saveManager.unlockSkin(skin.id)
                                unlockedSkins[skin.id] = true
                                playerCoins = saveManager.getCoins()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SkinItemRow(
    skin: Skin,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isEquipped) Color(0x1A00E5FF) else Color(0x12FFFFFF))
            .border(
                1.dp,
                if (isEquipped) Color(0xFF00E5FF) else Color(0x1AFFFFFF),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing Orb Visual Representation
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color(skin.primaryColor)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2.2f
                val center = Offset(size.width / 2f, size.height / 2f)
                
                // Outer glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(skin.primaryColor), Color.Transparent),
                        center = center,
                        radius = r * 1.8f
                    ),
                    center = center,
                    radius = r * 1.8f
                )

                // Inner core
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(skin.primaryColor), Color(skin.secondaryColor)),
                        start = Offset(center.x - r, center.y - r),
                        end = Offset(center.x + r, center.y + r)
                    ),
                    center = center,
                    radius = r
                )

                when (skin.id) {
                    "kitty_pop" -> {
                        val earSize = r * 0.62f
                        drawPath(
                            path = Path().apply {
                                moveTo(center.x - r * 0.75f, center.y - r * 0.5f)
                                lineTo(center.x - r * 0.35f, center.y - r * 1.35f)
                                lineTo(center.x - r * 0.05f, center.y - r * 0.45f)
                                close()
                            },
                            color = Color(skin.primaryColor)
                        )
                        drawPath(
                            path = Path().apply {
                                moveTo(center.x + r * 0.75f, center.y - r * 0.5f)
                                lineTo(center.x + r * 0.35f, center.y - r * 1.35f)
                                lineTo(center.x + r * 0.05f, center.y - r * 0.45f)
                                close()
                            },
                            color = Color(skin.primaryColor)
                        )
                        drawCircle(Color(0xFFFF5FB7), radius = earSize * 0.25f, center = Offset(center.x + r * 0.35f, center.y - r * 0.55f))
                    }
                    "dragon_fire" -> {
                        drawPath(
                            path = Path().apply {
                                moveTo(center.x - r * 0.5f, center.y - r * 0.85f)
                                lineTo(center.x - r * 0.25f, center.y - r * 1.55f)
                                lineTo(center.x, center.y - r * 0.9f)
                                lineTo(center.x + r * 0.25f, center.y - r * 1.55f)
                                lineTo(center.x + r * 0.5f, center.y - r * 0.85f)
                            },
                            color = Color(0xFFFFD166)
                        )
                        drawPath(
                            path = Path().apply {
                                moveTo(center.x - r * 1.0f, center.y)
                                lineTo(center.x - r * 1.75f, center.y - r * 0.45f)
                                lineTo(center.x - r * 1.25f, center.y + r * 0.45f)
                                close()
                            },
                            color = Color(0xAAFFB000)
                        )
                        drawPath(
                            path = Path().apply {
                                moveTo(center.x + r * 1.0f, center.y)
                                lineTo(center.x + r * 1.75f, center.y - r * 0.45f)
                                lineTo(center.x + r * 1.25f, center.y + r * 0.45f)
                                close()
                            },
                            color = Color(0xAAFFB000)
                        )
                    }
                    "dino_mint" -> {
                        for (i in -2..2) {
                            drawPath(
                                path = Path().apply {
                                    val x = center.x + i * r * 0.28f
                                    moveTo(x - r * 0.12f, center.y - r * 0.82f)
                                    lineTo(x, center.y - r * 1.28f)
                                    lineTo(x + r * 0.12f, center.y - r * 0.82f)
                                    close()
                                },
                                color = Color(0xFFB9FFD0)
                            )
                        }
                    }
                    "shadow_ninja" -> {
                        drawRect(
                            color = Color(0xDD050712),
                            topLeft = Offset(center.x - r * 0.85f, center.y - r * 0.22f),
                            size = androidx.compose.ui.geometry.Size(r * 1.7f, r * 0.44f)
                        )
                        drawCircle(Color(0xFF8C5CFF), radius = r * 0.12f, center = Offset(center.x - r * 0.28f, center.y))
                        drawCircle(Color(0xFF8C5CFF), radius = r * 0.12f, center = Offset(center.x + r * 0.28f, center.y))
                    }
                    "royal_panda" -> {
                        drawCircle(Color(0xFF20242E), radius = r * 0.32f, center = Offset(center.x - r * 0.62f, center.y - r * 0.62f))
                        drawCircle(Color(0xFF20242E), radius = r * 0.32f, center = Offset(center.x + r * 0.62f, center.y - r * 0.62f))
                        drawCircle(Color(0xFFFFD166), radius = r * 0.18f, center = Offset(center.x, center.y - r * 0.15f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Skin Info details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                skin.displayName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                skin.description,
                color = Color(0xFF90A4AE),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Purchase / Equip button
        if (isEquipped) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x3300E5FF))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("EQUIPPED", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        } else if (isUnlocked) {
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("EQUIP", color = Color(0xFF140D25), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            // Locked: show buy button with cost
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("${skin.cost} COINS", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
