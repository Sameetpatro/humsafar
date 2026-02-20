// app/src/main/java/com/example/humsafar/navigation/AppNavigation.kt
// REPLACES EXISTING FILE
//
// CHANGES vs original:
//   + "settings" route → SettingsScreen
//   + "voice/{siteName}/{siteId}" route → VoiceChatScreen
//   + voiceChatRoute() helper for URL-encoding site names with spaces

package com.example.humsafar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.humsafar.ui.LoginScreen
import com.example.humsafar.ui.SignUpScreen
import com.example.humsafar.ui.MapScreen
import com.example.humsafar.ui.SettingsScreen
import com.example.humsafar.ui.VoiceChatScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onSignupClick = { navController.navigate("signup") },
                onBypassClick = { navController.navigate("home") }
            )
        }

        composable("signup") {
            SignUpScreen(
                onLoginClick  = { navController.popBackStack() },
                onBypassClick = { navController.navigate("home") }
            )
        }

        composable("home") {
            MapScreen(
                onNavigateToVoice = { name, id ->
                    navController.navigate(voiceChatRoute(name, id))
                }
            )
        }

        // Settings — language selection, reachable from VoiceChatScreen top bar
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // Voice chat — site name + id encoded in path
        composable(
            route     = "voice/{siteName}/{siteId}",
            arguments = listOf(
                navArgument("siteName") { type = NavType.StringType },
                navArgument("siteId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val siteName = backStack.arguments?.getString("siteName") ?: "Heritage Site"
            val siteId   = backStack.arguments?.getString("siteId")   ?: ""

            VoiceChatScreen(
                siteName             = siteName,
                siteId               = siteId,
                onBack               = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
    }
}

/**
 * URL-encodes the site name for use as a Nav path segment.
 * Handles spaces and special characters in names like "Qutub Minar" or "Gateway of India".
 */
fun voiceChatRoute(siteName: String, siteId: String): String {
    val encoded = java.net.URLEncoder.encode(siteName, "UTF-8")
    return "voice/$encoded/$siteId"
}