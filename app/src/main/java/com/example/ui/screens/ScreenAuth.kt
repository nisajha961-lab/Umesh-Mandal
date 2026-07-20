package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenAuth(
    viewModel: AppViewModel,
    onLoginSuccess: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(24.dp)
                .background(Color(0xFF0D0D1F), shape = RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonMagenta)),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cyberpunk glowing title
            Text(
                text = "FIRING FOR CASH",
                fontSize = 26.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "SECURE LOG_IN GATEWAY",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonMagenta,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Input Fields
            OutlinedTextField(
                value = emailInput,
                onValueChange = { 
                    emailInput = it 
                    errorText = null
                },
                label = { Text("CYBER_EMAIL_ADDRESS", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0xFF32324C),
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { 
                    passwordInput = it 
                    errorText = null
                },
                label = { Text("ENCRYPTED_PASSCODE", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonMagenta) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonMagenta,
                    unfocusedBorderColor = Color(0xFF32324C),
                    focusedLabelColor = NeonMagenta,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = Color.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    if (emailInput.isBlank() || passwordInput.isBlank()) {
                        errorText = "ERR: CREDENTIALS CANNOT BE BLANK"
                        viewModel.playTapSound()
                    } else {
                        val success = viewModel.loginUser(emailInput)
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorText = "ERR: INVALID EMAIL SYNTAX"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(NeonCyan, NeonMagenta)),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.15f), NeonMagenta.copy(alpha = 0.15f))),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ESTABLISH_CONNECTION",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ENCRYPTION STATUS: ACTIVE AES_256",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
        }
    }
}
