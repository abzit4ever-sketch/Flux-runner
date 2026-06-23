package com.example.fluxrunner.ui.earn

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.fluxrunner.logic.SaveManager

@Composable
fun WithdrawCashScreen(
    saveManager: SaveManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cash by remember { mutableStateOf(saveManager.getRealCash()) }
    var amount by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("PayPal") }
    var feedback by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF061208), Color(0xFF07111F))))
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
                    "WITHDRAW CASH",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(Color(0xFF00FF66), Offset.Zero, 14f)
                    )
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xCC0B1020))
                    .border(1.dp, Color(0x6600FF66), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Text("AVAILABLE CASH", color = Color(0xFF8EA3B4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(String.format("$%.2f USD", cash), color = Color(0xFF00FF66), fontSize = 34.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This screen is ready for a payout provider, but live withdrawals require a secure backend, identity checks, provider keys, and app-store compliant terms.",
                    color = Color(0xFF8EA3B4),
                    fontSize = 12.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x99081218))
                    .border(1.dp, Color(0x224A5B75), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("PAYOUT METHOD", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("PayPal", "Google Pay", "Bank").forEach { method ->
                        val active = method == selectedMethod
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(0xFF00FF66) else Color(0xFF111827))
                                .border(1.dp, if (active) Color(0xFF00FF66) else Color(0x334A5B75), RoundedCornerShape(10.dp))
                                .clickable { selectedMethod = method },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(method, color = if (active) Color(0xFF061208) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Amount, e.g. 5.00", color = Color(0xFF5A6A7A)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FF66),
                        unfocusedBorderColor = Color(0x334A5B75),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF00FF66)
                    )
                )

                Button(
                    onClick = {
                        val value = amount.toFloatOrNull()
                        feedback = when {
                            value == null || value <= 0f -> "Enter a valid amount."
                            value > cash -> "Not enough cash available."
                            else -> "Payout provider is not connected yet."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66))
                ) {
                    Text("REQUEST WITHDRAWAL", color = Color(0xFF061208), fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x334A5B75))
                ) {
                    Text("BACK TO MENU", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (feedback.isNotBlank()) {
                Text(
                    feedback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xDD07111F))
                        .border(1.dp, Color(0x4400FF66), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    color = Color(0xFF00FF66),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
