package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ScreenDashboard(
    viewModel: AppViewModel,
    onLaunchGame: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    var titleTaps by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    // Simulated banner background ticking loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000) // update banner impression every 15 seconds
            viewModel.simulateBannerImpression()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Header Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        titleTaps++
                        if (titleTaps >= 5) {
                            titleTaps = 0
                            onNavigateToAdmin()
                        }
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "FIRING FOR ",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "CASH",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonMagenta
                    )
                }
                Text(
                    text = viewModel.systemStatusText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (viewModel.isOnline) NeonGreen else Color.Red
                )
            }

            // Audio sound state button
            Button(
                onClick = { viewModel.toggleSound() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(30.dp)
                    .border(
                        width = 1.dp,
                        color = if (viewModel.isSoundOn) NeonCyan else NeonMagenta,
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                Text(
                    text = if (viewModel.isSoundOn) "SOUND: ON" else "SOUND: OFF",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (viewModel.isSoundOn) NeonCyan else NeonMagenta
                )
            }
        }

        // 2. User info and statistics section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, Color(0xFF1E1E3F), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PILOT_IDENT: ${viewModel.userEmail}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CYBER_COINS_BALANCE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                        Text(
                            text = "${viewModel.localCoins} CC",
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = "VALUATION: ₹${String.format("%.2f", viewModel.localCoins * 0.10)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "HIGH_SCORE_RECORD",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonYellow
                        )
                        Text(
                            text = "${viewModel.highScore} PTS",
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonYellow
                        )
                        Text(
                            text = "CONV: 20 SHOTS = 1CC",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Central big flashing Start Game button
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val buttonScale by infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Button(
            onClick = {
                viewModel.playTapSound()
                onLaunchGame()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonGreen)),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.2f), NeonGreen.copy(alpha = 0.2f))),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAUNCH GAME TERMINAL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Simulated Ads Performance Table
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, Color(0xFF1E1E3F), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SIMULATED AD_REVENUE SUITE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("FORMAT", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.weight(1.5f))
                    Text("IMPS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text("eCPM", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text("EST. PAY", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }

                Divider(color = Color(0xFF1E1E3F), thickness = 1.dp)

                // Row: Banner
                AdRow(
                    format = "Banner Ad",
                    imps = viewModel.bannerImpressions,
                    ecpm = "₹10.00",
                    revenue = viewModel.bannerImpressions * 0.01
                )

                // Row: Interstitial
                AdRow(
                    format = "Interstitial",
                    imps = viewModel.interstitialImpressions,
                    ecpm = "₹30.00",
                    revenue = viewModel.interstitialImpressions * 0.03
                )

                // Row: Rewarded
                AdRow(
                    format = "Rewarded Video",
                    imps = viewModel.rewardedImpressions,
                    ecpm = "₹70.00",
                    revenue = viewModel.rewardedImpressions * 0.07
                )

                Divider(color = Color(0xFF1E1E3F), thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

                // Total Estimated Ads Revenue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL SIMULATED REVENUE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "₹${String.format("%.2f", viewModel.estimatedRevenue)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
            }
        }

        // Logout & Leaderboard Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    viewModel.refreshLeaderboard()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E3F)),
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PULL_SYNC", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
            }

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331422)),
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            ) {
                Text("DISCONNECT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = NeonMagenta)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Simulated Banner Container at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .background(Color(0xFF070714), shape = RoundedCornerShape(6.dp))
                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SIMULATED_BANNER_AD_SLOT",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Text(
                    text = "ca-app-pub-4720255455581147/7317442721",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AdRow(
    format: String,
    imps: Int,
    ecpm: String,
    revenue: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(format, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White, modifier = Modifier.weight(1.5f))
        Text("$imps", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = OnDarkText, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(ecpm, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text("₹${String.format("%.2f", revenue)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = NeonGreen, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}
