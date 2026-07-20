package com.example.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.theme.*

@Composable
fun ScreenGame(
    viewModel: AppViewModel,
    onExitGame: () -> Unit
) {
    val bulletsState = viewModel.bullets
    val targetsState = viewModel.targets
    val particlesState = viewModel.particles

    // Simple flashing color for anti-bot HUD banner
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val bannerColor by infiniteTransition.animateColor(
        initialValue = Color.Yellow,
        targetValue = NeonMagenta,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        // 1. Core Space Shooter Canvas Viewport
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val sensitivity = 1.0f / size.width
                        val newX = (viewModel.playerX + dragAmount.x * sensitivity).coerceIn(0.04f, 0.96f)
                        viewModel.playerX = newX
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // A. Draw retro starry background grid
            val gridSpacing = 60f
            for (x in 0..(width / gridSpacing).toInt()) {
                drawLine(
                    color = Color(0xFF0F0F26),
                    start = Offset(x * gridSpacing, 0f),
                    end = Offset(x * gridSpacing, height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(height / gridSpacing).toInt()) {
                drawLine(
                    color = Color(0xFF0F0F26),
                    start = Offset(0f, y * gridSpacing),
                    end = Offset(width, y * gridSpacing),
                    strokeWidth = 1f
                )
            }

            // B. Draw falling Targets
            for (target in targetsState.value) {
                val tx = target.x * width
                val ty = target.y * height
                val tr = target.radius * width
                drawCircle(
                    color = NeonMagenta,
                    radius = tr,
                    center = Offset(tx, ty),
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = NeonMagenta.copy(alpha = 0.2f),
                    radius = tr * 0.7f,
                    center = Offset(tx, ty)
                )
            }

            // C. Draw laser Bullets
            for (bullet in bulletsState.value) {
                val bx = bullet.x * width
                val by = bullet.y * height
                drawRect(
                    color = NeonCyan,
                    topLeft = Offset(bx - 3f, by - 12f),
                    size = Size(6f, 24f)
                )
            }

            // D. Draw Explosion Particles
            for (p in particlesState.value) {
                val px = p.x * width
                val py = p.y * height
                val color = when (p.colorType) {
                    0 -> NeonCyan
                    1 -> NeonMagenta
                    else -> NeonYellow
                }.copy(alpha = p.life)
                drawCircle(
                    color = color,
                    radius = 3f + (1f - p.life) * 6f,
                    center = Offset(px, py)
                )
            }

            // E. Draw Player Ship
            val px = viewModel.playerX * width
            val py = 0.88f * height
            val shipWidth = 0.08f * width
            val shipHeight = 0.07f * height

            val path = Path().apply {
                moveTo(px, py - shipHeight / 2) // Nose
                lineTo(px - shipWidth / 2, py + shipHeight / 2) // Bottom left
                lineTo(px + shipWidth / 2, py + shipHeight / 2) // Bottom right
                close()
            }
            drawPath(
                path = path,
                color = NeonCyan,
                style = Stroke(width = 4f)
            )
            drawPath(
                path = path,
                color = NeonCyan.copy(alpha = 0.15f)
            )
        }

        // 2. Real-time HUD stats overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SESSION SCORE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    Text("${viewModel.gameScore} PTS", fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("COINS EARNED", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                    Text("+${viewModel.coinsEarnedInSession} CC", fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cooldown Cap Display
            if (viewModel.isCapReached) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(bannerColor.copy(alpha = 0.15f))
                        .border(1.dp, bannerColor, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CAP_REACHED (COOLDOWN): COIN BALANCES LOCKED",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = bannerColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Drag instructions at bottom HUD
        Text(
            text = "<< DRAG HORIZONTALLY TO ALIGN SHIP >>",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )

        // 3. Game Over popup Modal
        if (viewModel.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(DarkSurface, shape = RoundedCornerShape(16.dp))
                        .border(2.dp, NeonMagenta, shape = RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CONNECTION TERMINATED",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonMagenta,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "YOUR SHIP WENT CRITICAL",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FINAL_SCORE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Text("${viewModel.gameScore} PTS", fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonYellow)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COIN_YIELD", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Text("+${viewModel.coinsEarnedInSession} CC", fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons
                    Button(
                        onClick = { viewModel.handleRevive() },
                        enabled = viewModel.currentRevivesUsed < 4,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .border(
                                width = 1.dp,
                                color = if (viewModel.currentRevivesUsed < 4) NeonCyan else Color.DarkGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (viewModel.currentRevivesUsed < 4) NeonCyan.copy(alpha = 0.1f) else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "REVIVE SHIP [${viewModel.currentRevivesUsed}/4] (WATCH AD)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (viewModel.currentRevivesUsed < 4) NeonCyan else Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.handleDoubleCoins() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .border(1.dp, NeonGreen, shape = RoundedCornerShape(8.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NeonGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DOUBLE SESSION YIELD (WATCH AD)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onExitGame,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331422)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "EXIT GAME TERMINAL",
                            fontFamily = FontFamily.Monospace,
                            color = NeonMagenta,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 4. Simulated Fullscreen AD overlays
        if (viewModel.activeSimAdOverlay != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = if (viewModel.activeSimAdOverlay == "INTERSTITIAL") NeonCyan else NeonMagenta,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (viewModel.activeSimAdOverlay == "INTERSTITIAL") {
                            "SIMULATING INTERSTITIAL ADS"
                        } else {
                            "SIMULATING REWARDED VIDEO ADS"
                        },
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "PRODUCED BY GOOGLE ADMOB SIMULATION",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Text(
                        text = "SECONDS REMAINING: ${viewModel.simAdProgressSeconds}s",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.activeSimAdOverlay == "INTERSTITIAL") NeonCyan else NeonGreen
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "ca-app-pub-4720255455581147 / 7317442721",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
