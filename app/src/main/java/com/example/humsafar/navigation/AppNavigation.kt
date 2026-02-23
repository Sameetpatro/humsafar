// app/src/main/java/com/example/humsafar/navigation/AppNavigation.kt
// UPDATED — adds "detail/{siteName}/{siteId}" and "profile" routes

package com.example.humsafar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.humsafar.ui.LoginScreen
import com.example.humsafar.ui.SignUpScreen
import com.example.humsafar.ui.MapScreen
import com.example.humsafar.ui.HeritageDetailScreen
import com.example.humsafar.ui.ProfileScreen
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
                onNavigateToVoice   = { name, id -> navController.navigate(voiceChatRoute(name, id)) },
                onNavigateToDetail  = { name, id -> navController.navigate(heritageDetailRoute(name, id)) },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        // Heritage detail — article + 4 action bubbles
        composable(
            route     = "detail/{siteName}/{siteId}",
            arguments = listOf(
                navArgument("siteName") { type = NavType.StringType },
                navArgument("siteId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val siteName = backStack.arguments?.getString("siteName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Heritage Site"
            val siteId   = backStack.arguments?.getString("siteId") ?: ""

            HeritageDetailScreen(
                siteName          = siteName,
                siteId            = siteId,
                onBack            = { navController.popBackStack() },
                onNavigateToVoice = { name, id ->
                    navController.navigate(voiceChatRoute(name, id))
                }
            )
        }

        // Profile + Settings + About
        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

        // Voice chat
        composable(
            route     = "voice/{siteName}/{siteId}",
            arguments = listOf(
                navArgument("siteName") { type = NavType.StringType },
                navArgument("siteId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val siteName = backStack.arguments?.getString("siteName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Heritage Site"
            val siteId   = backStack.arguments?.getString("siteId") ?: ""

            VoiceChatScreen(
                siteName             = siteName,
                siteId               = siteId,
                onBack               = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("profile") }
            )
        }
    }
}

fun heritageDetailRoute(siteName: String, siteId: String): String {
    val encoded = java.net.URLEncoder.encode(siteName, "UTF-8")
    return "detail/$encoded/$siteId"
}

fun voiceChatRoute(siteName: String, siteId: String): String {
    val encoded = java.net.URLEncoder.encode(siteName, "UTF-8")
    return "voice/$encoded/$siteId"
}