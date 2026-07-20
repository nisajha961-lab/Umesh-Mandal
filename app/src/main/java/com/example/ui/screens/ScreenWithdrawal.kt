package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenWithdrawal(viewModel: AppViewModel) {
    var selectedMethod by remember { mutableStateOf("UPI") } // "UPI", "PAYTM", "PAYPAL"
    var gatewayInput by remember { mutableStateOf("") }
    var coinsInput by remember { mutableStateOf("") }

    var errorText by remember { mutableStateOf<String?>(null) }
    var showLockedDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Dynamic Computations
    val coinsToWithdraw = coinsInput.toIntOrNull() ?: 0
    val rawValueInRps = coinsToWithdraw * 0.10
    val taxDeduction = rawValueInRps * 0.30
    val netYield = rawValueInRps - taxDeduction

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "EXCHANGE GATEWAY",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "CONVERT VIRTUAL COINS INTO NATIVE CASH",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonGreen
                )
            }
        }

        Divider(color = Color(0xFF1E1E3F), thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))

        // Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TOTAL_CYBER_COINS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    Text("${viewModel.localCoins} CC", fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonGreen)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL_EST_WORTH", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    Text("₹${String.format("%.2f", viewModel.localCoins * 0.10)}", fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // 2. Withdrawal inputs Form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1E1E3F), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "REDEEM METHOD",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Tab Selector for methods
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "PAYTM", "PAYPAL").forEach { method ->
                        val isSelected = selectedMethod == method
                        Button(
                            onClick = { 
                                viewModel.playTapSound()
                                selectedMethod = method 
                                gatewayInput = ""
                                errorText = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF1E1E3F) else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else Color(0xFF282846),
                                    shape = RoundedCornerShape(6.dp)
                                ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = method,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) NeonCyan else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Field 1: Amount in coins
                OutlinedTextField(
                    value = coinsInput,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            coinsInput = it
                            errorText = null
                        }
                    },
                    label = { Text("CC_REDEMPTION_QUANTITY", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color(0xFF32324C),
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Field 2: Gateway address
                val labelText = when (selectedMethod) {
                    "UPI" -> "VIRTUAL_UPI_ADDRESS (user@provider)"
                    "PAYTM" -> "PAYTM_MOBILE_NUMBER (10 Digits)"
                    else -> "PAYPAL_RECEIVER_EMAIL"
                }

                OutlinedTextField(
                    value = gatewayInput,
                    onValueChange = {
                        gatewayInput = it
                        errorText = null
                    },
                    label = { Text(labelText, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF32324C),
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = if (selectedMethod == "PAYTM") KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Real-time tax computation displays
                Text(
                    text = "TAX_DEDUCTIONS & CONVERSIONS",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Conversion Rate", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    Text("1 CC = ₹0.10", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Raw Valuation", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    Text("₹${String.format("%.2f", rawValueInRps)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Govt simulated Tax (30%)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    Text("- ₹${String.format("%.2f", taxDeduction)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = NeonMagenta)
                }

                Divider(color = Color(0xFF1E1E3F), thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EST. NET_YIELD", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonGreen)
                    Text("₹${String.format("%.2f", netYield)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonGreen)
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = Color.Red,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                // Submit Button
                Button(
                    onClick = {
                        viewModel.playTapSound()
                        // Validation
                        if (coinsToWithdraw <= 0) {
                            errorText = "ERR: AMOUNT MUST BE GREATER THAN ZERO"
                        } else if (coinsToWithdraw > viewModel.localCoins) {
                            errorText = "ERR: INSUFFICIENT COINS BALANCE"
                        } else if (gatewayInput.isBlank()) {
                            errorText = "ERR: ADDRESS VALUE REQUIRED"
                        } else {
                            // Method-specific regex validation
                            val isValid = when (selectedMethod) {
                                "UPI" -> {
                                    val regex = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$".toRegex()
                                    regex.matches(gatewayInput.trim())
                                }
                                "PAYTM" -> {
                                    val regex = "^[6-9]\\d{9}$".toRegex()
                                    regex.matches(gatewayInput.trim())
                                }
                                else -> {
                                    android.util.Patterns.EMAIL_ADDRESS.matcher(gatewayInput.trim()).matches()
                                }
                            }

                            if (!isValid) {
                                errorText = "ERR: INVALID $selectedMethod ADDRESS FORMAT"
                            } else {
                                // Time Lock Check: LOCKED until 30th
                                val calendar = Calendar.getInstance()
                                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                                if (dayOfMonth < 30) {
                                    showLockedDialog = true
                                } else {
                                    // Payout successful! Deduct coins locally
                                    viewModel.adminSetCoins(viewModel.localCoins - coinsToWithdraw)
                                    showSuccessDialog = true
                                    coinsInput = ""
                                    gatewayInput = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(45.dp)
                        .border(1.dp, NeonGreen, shape = RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ESTABLISH_REDEEM_GATE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Lock Info Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "EXCHANGE RELEASES ACTIVE ONLY ON THE 30TH",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
        }
    }

    // Dialog 1: Time Locked Alert
    if (showLockedDialog) {
        AlertDialog(
            onDismissRequest = { showLockedDialog = false },
            title = {
                Text(
                    "TRANSACTION SUSPENDED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta
                )
            },
            text = {
                Text(
                    "EXCHANGE WINDOW LOCKED until the 30th of the current month.\n\nSimulated gateways remain disabled until synchronization locks release on calendar release day.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.playTapSound()
                        showLockedDialog = false
                    }
                ) {
                    Text("UNDERSTOOD", fontFamily = FontFamily.Monospace, color = NeonMagenta)
                }
            },
            containerColor = DarkSurface,
            modifier = Modifier.border(1.dp, NeonMagenta, RoundedCornerShape(8.dp))
        )
    }

    // Dialog 2: Success Redemptions
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Text(
                    "EXCHANGE COMPLETE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            },
            text = {
                Text(
                    "Redemption request successfully submitted!\n\nNet yield of ₹${String.format("%.2f", netYield)} (after 30% tax) has been simulated for transfer to: $gatewayInput",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.playTapSound()
                        showSuccessDialog = false
                    }
                ) {
                    Text("OK", fontFamily = FontFamily.Monospace, color = NeonGreen)
                }
            },
            containerColor = DarkSurface,
            modifier = Modifier.border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
        )
    }
}
