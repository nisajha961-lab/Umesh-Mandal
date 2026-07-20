package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenAdmin(
    viewModel: AppViewModel,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    var isAuthorized by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var coinsAdjustmentInput by remember { mutableStateOf("") }

    if (!isAuthorized) {
        // Render PIN Gate Terminal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(DarkSurface, shape = RoundedCornerShape(12.dp))
                    .border(2.dp, NeonYellow, shape = RoundedCornerShape(12.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "RESTRICTED TERMINAL",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonYellow
                )
                Text(
                    text = "ENTER SYSTEM AUTHORIZATION_PIN",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) {
                            pinInput = it
                            pinError = null
                        }
                    },
                    label = { Text("SECURITY_PIN_GATE", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonYellow,
                        unfocusedBorderColor = Color(0xFF32324C),
                        focusedLabelColor = NeonYellow,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true
                )

                if (pinError != null) {
                    Text(
                        text = pinError!!,
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onBackToDashboard,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282846)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ABORT", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.playTapSound()
                            if (pinInput == "1041") {
                                isAuthorized = true
                            } else {
                                pinError = "ACCESS_DENIED: INCORRECT PIN"
                                pinInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DECRYPT", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Render Authorized Admin Panel Terminal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBlack)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DIAGNOSTICS PANEL",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "AUTHORIZED OPERATOR VIEWPORT",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonYellow
                    )
                }
            }

            Divider(color = Color(0xFF1E1E3F), thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))

            // Section 1: Metrics Diagnostics
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, NeonYellow.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE PERFORMANCE METRICS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonYellow,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    MetricItem(label = "ACTIVE_USER_PILOT", value = viewModel.userEmail)
                    MetricItem(label = "DEVICE_NETWORK_STATUS", value = viewModel.systemStatusText)
                    MetricItem(label = "SLIDING_WINDOW_CAP_BLOCKED", value = if (viewModel.isCapReached) "YES (CAP_REACHED)" else "NO")
                    
                    val coinsHistoryCount = viewModel.sharedPrefs.getCoinEarningHistory().size
                    MetricItem(label = "SLIDING_WINDOW_TX_COUNT", value = "$coinsHistoryCount / 20 CC")
                    
                    MetricItem(label = "TOTAL_BANNER_IMPRESSIONS", value = "${viewModel.bannerImpressions} imps")
                    MetricItem(label = "TOTAL_INTER_IMPRESSIONS", value = "${viewModel.interstitialImpressions} imps")
                    MetricItem(label = "TOTAL_REWARD_IMPRESSIONS", value = "${viewModel.rewardedImpressions} imps")
                    MetricItem(label = "CALCULATED_MOCK_REVENUE", value = "₹${String.format("%.2f", viewModel.estimatedRevenue)}")
                }
            }

            // Section 2: Force coin adjuster input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0xFF1E1E3F), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "COINS BALANCE MODIFICATION",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = coinsAdjustmentInput,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                coinsAdjustmentInput = it
                            }
                        },
                        label = { Text("FORCE_SET_CC_QUANTITY", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF32324C),
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val valAmt = coinsAdjustmentInput.toIntOrNull()
                            if (valAmt != null) {
                                viewModel.adminSetCoins(valAmt)
                                Toast.makeText(context, "Balance adjusted successfully to: $valAmt CC", Toast.LENGTH_SHORT).show()
                                coinsAdjustmentInput = ""
                            } else {
                                Toast.makeText(context, "Invalid Amount Entered", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "FORCE_APPLY_OVERRIDE",
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Diagnostic export and return controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val file = viewModel.exportDiagnosticLogsJson(context)
                        if (file != null) {
                            Toast.makeText(context, "JSON log compiled at: ${file.name}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Log compilation failed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E3F)),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COMPILE_JSON", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }

                Button(
                    onClick = onBackToDashboard,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331422)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CLOSE_PORT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = NeonMagenta)
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
        Text(text = value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
