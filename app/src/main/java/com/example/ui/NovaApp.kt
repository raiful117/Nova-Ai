package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AccessibilitySetupScreen
import com.example.ui.screens.ApiKeySetupScreen
import com.example.ui.screens.ChatModeScreen
import com.example.ui.screens.HandsFreeCenterScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageSettingsScreen
import com.example.ui.screens.MemoryManagerScreen
import com.example.ui.screens.MicrophonePermissionScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OverlaySettingsScreen
import com.example.ui.screens.PrivacyOfflineScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskPlannerScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.NovaBorder
import com.example.ui.theme.NovaCyanGlow
import com.example.ui.theme.NovaDarkNavy
import com.example.ui.theme.NovaElectricPurple
import com.example.ui.theme.NovaNearBlack
import com.example.ui.theme.NovaSurface
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Welcome : Screen("welcome", "Welcome")
    object Onboarding : Screen("onboarding", "Onboarding")
    object ApiSetup : Screen("api_setup", "Configure API")
    object MicPermission : Screen("mic_permission", "Microphone")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Assignment)
    object History : Screen("history", "History", Icons.Default.History)
    object SettingsHub : Screen("settings_hub", "Settings", Icons.Default.Settings)
    object Chat : Screen("chat", "Chat")
    object LanguageSettings : Screen("settings_language", "Language")
    object VoiceSettings : Screen("settings_voice", "Voice")
    object HandsFreeCenter : Screen("settings_handsfree", "Hands-Free")
    object OverlaySettings : Screen("settings_overlay", "Overlay")
    object AccessibilitySettings : Screen("settings_accessibility", "Accessibility")
    object Reminders : Screen("settings_reminders", "Reminders")
    object Memory : Screen("settings_memory", "Memory")
    object PrivacyOffline : Screen("settings_privacy", "Privacy")
    object About : Screen("settings_about", "About")
}

@Composable
fun NovaApp(viewModel: NovaMainViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userPrefs by viewModel.userPreferences.collectAsState()

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Tasks,
        Screen.History,
        Screen.SettingsHub
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar(
                    containerColor = NovaSurface,
                    contentColor = NovaTextPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("nova_bottom_navigation")
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) NovaCyanGlow else NovaTextSecondary
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    color = if (isSelected) NovaCyanGlow else NovaTextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = NovaSurfaceElevated
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NovaNearBlack)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (userPrefs.onboardingCompleted) Screen.Home.route else Screen.Welcome.route
            ) {
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onGetStarted = {
                            navController.navigate(Screen.Onboarding.route)
                        },
                        onConfigureApi = {
                            navController.navigate(Screen.ApiSetup.route)
                        }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onFinish = {
                            viewModel.setOnboardingCompleted(true)
                            navController.navigate(Screen.MicPermission.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.MicPermission.route) {
                    MicrophonePermissionScreen(
                        onPermissionGranted = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.MicPermission.route) { inclusive = true }
                            }
                        },
                        onSkip = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.MicPermission.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.ApiSetup.route) {
                    ApiKeySetupScreen(
                        viewModel = viewModel,
                        onFinished = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                        onNavigateToHistory = { navController.navigate(Screen.History.route) },
                        onNavigateToSettings = { navController.navigate(Screen.SettingsHub.route) },
                        onNavigateToHandsFreeCenter = { navController.navigate(Screen.HandsFreeCenter.route) }
                    )
                }

                composable(Screen.Chat.route) {
                    ChatModeScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Tasks.route) {
                    TaskPlannerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.navigate(Screen.Home.route) }
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.navigate(Screen.Home.route) },
                        onSelectConversation = { question ->
                            viewModel.handleUserInput(question)
                            navController.navigate(Screen.Home.route)
                        }
                    )
                }

                composable(Screen.SettingsHub.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.navigate(Screen.Home.route) },
                        onNavigateToLanguage = { navController.navigate(Screen.LanguageSettings.route) },
                        onNavigateToVoice = { navController.navigate(Screen.VoiceSettings.route) },
                        onNavigateToApi = { navController.navigate(Screen.ApiSetup.route) },
                        onNavigateToHandsFree = { navController.navigate(Screen.HandsFreeCenter.route) },
                        onNavigateToOverlay = { navController.navigate(Screen.OverlaySettings.route) },
                        onNavigateToAccessibility = { navController.navigate(Screen.AccessibilitySettings.route) },
                        onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                        onNavigateToMemory = { navController.navigate(Screen.Memory.route) },
                        onNavigateToPrivacyOffline = { navController.navigate(Screen.PrivacyOffline.route) },
                        onNavigateToAbout = { navController.navigate(Screen.About.route) }
                    )
                }

                composable(Screen.LanguageSettings.route) {
                    LanguageSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.VoiceSettings.route) {
                    VoiceSettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.HandsFreeCenter.route) {
                    HandsFreeCenterScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.OverlaySettings.route) {
                    OverlaySettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AccessibilitySettings.route) {
                    AccessibilitySetupScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Reminders.route) {
                    RemindersScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Memory.route) {
                    MemoryManagerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.PrivacyOffline.route) {
                    PrivacyOfflineScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.About.route) {
                    AboutScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
