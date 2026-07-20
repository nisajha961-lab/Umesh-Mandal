package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ScreenAdmin
import com.example.ui.screens.ScreenAuth
import com.example.ui.screens.ScreenDashboard
import com.example.ui.screens.ScreenGame
import com.example.ui.screens.ScreenLeaderboard
import com.example.ui.screens.ScreenWithdrawal
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan

object Routes {
    const val AUTH = "auth"
    const val DASHBOARD = "dashboard"
    const val GAME = "game"
    const val LEADERBOARD = "leaderboard"
    const val WITHDRAWAL = "withdrawal"
    const val ADMIN = "admin"
}

@Composable
fun AppNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.AUTH

    // Bottom Navigation Bar is displayed ONLY on dashboard, leaderboard, and exchange tabs.
    val showBottomBar = currentRoute in listOf(Routes.DASHBOARD, Routes.LEADERBOARD, Routes.WITHDRAWAL)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    // Dashboard Item
                    NavigationBarItem(
                        selected = currentRoute == Routes.DASHBOARD,
                        onClick = {
                            viewModel.playTapSound()
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("DASHBOARD", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF191932)
                        )
                    )

                    // Leaderboard Item
                    NavigationBarItem(
                        selected = currentRoute == Routes.LEADERBOARD,
                        onClick = {
                            viewModel.playTapSound()
                            navController.navigate(Routes.LEADERBOARD) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard") },
                        label = { Text("LEADERBOARD", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF191932)
                        )
                    )

                    // Exchange/Withdrawal Item
                    NavigationBarItem(
                        selected = currentRoute == Routes.WITHDRAWAL,
                        onClick = {
                            viewModel.playTapSound()
                            navController.navigate(Routes.WITHDRAWAL) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Exchange") },
                        label = { Text("EXCHANGE", fontFamily = FontFamily.Monospace, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF191932)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (viewModel.isLoggedIn) Routes.DASHBOARD else Routes.AUTH,
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBlack)
                .padding(innerPadding)
        ) {
            composable(Routes.AUTH) {
                ScreenAuth(viewModel) {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            }

            composable(Routes.DASHBOARD) {
                ScreenDashboard(
                    viewModel = viewModel,
                    onLaunchGame = {
                        viewModel.launchNewGame()
                        navController.navigate(Routes.GAME)
                    },
                    onNavigateToAdmin = {
                        navController.navigate(Routes.ADMIN)
                    },
                    onLogout = {
                        viewModel.logoutUser()
                        navController.navigate(Routes.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.GAME) {
                ScreenGame(viewModel) {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                }
            }

            composable(Routes.LEADERBOARD) {
                ScreenLeaderboard(viewModel)
            }

            composable(Routes.WITHDRAWAL) {
                ScreenWithdrawal(viewModel)
            }

            composable(Routes.ADMIN) {
                ScreenAdmin(viewModel) {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ADMIN) { inclusive = true }
                    }
                }
            }
        }
    }
}
