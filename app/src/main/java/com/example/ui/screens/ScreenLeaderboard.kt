package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.theme.*

@Composable
fun ScreenLeaderboard(viewModel: AppViewModel) {
    val scores by viewModel.leaderboardScores.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GLOBAL TERMINALS",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "PILOTS ROCKED IN LEADERBOARD",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan
                )
            }

            IconButton(
                onClick = {
                    viewModel.playTapSound()
                    viewModel.refreshLeaderboard()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh scores", tint = NeonCyan)
            }
        }

        Divider(color = Color(0xFF1E1E3F), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

        // 2. Scores list
        if (scores.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO HIGH SCORES SAVED YET",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "LAUNCH GAME TERMINAL TO RECORD SCORE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(scores) { index, entry ->
                    val rank = index + 1
                    val isCurrentUser = entry.email == viewModel.userEmail

                    val cardBorderBrush = if (isCurrentUser) {
                        Brush.linearGradient(listOf(NeonCyan, NeonMagenta))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF1E1E3F), Color(0xFF1E1E3F)))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isCurrentUser) 1.5.dp else 1.dp,
                                brush = cardBorderBrush,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentUser) Color(0xFF110B22) else DarkSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank Number
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = when (rank) {
                                            1 -> NeonYellow.copy(alpha = 0.2f)
                                            2 -> NeonCyan.copy(alpha = 0.2f)
                                            3 -> NeonMagenta.copy(alpha = 0.2f)
                                            else -> Color.DarkGray.copy(alpha = 0.15f)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when (rank) {
                                            1 -> NeonYellow
                                            2 -> NeonCyan
                                            3 -> NeonMagenta
                                            else -> Color.DarkGray
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#$rank",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when (rank) {
                                        1 -> NeonYellow
                                        2 -> NeonCyan
                                        3 -> NeonMagenta
                                        else -> Color.Gray
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Pilot Email / Name
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.username.uppercase(),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentUser) NeonCyan else Color.White
                                )
                                Text(
                                    text = if (entry.pendingSync) "OFFLINE_CACHED" else "SYNC_ESTABLISHED",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (entry.pendingSync) NeonMagenta else NeonGreen
                                )
                            }

                            // Score details
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${entry.highScore} PTS",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonYellow
                                )
                                Text(
                                    text = "${entry.coins} CC",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
