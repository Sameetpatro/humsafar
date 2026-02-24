// app/src/main/java/com/example/humsafar/navigation/AppNavigation.kt
// UPDATED — passes nodeId (Int) + siteId (Int) to NodeDetailScreen.
// QrScanScreen.onNodeReady now carries siteId too.

package com.example.humsafar.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.humsafar.data.TripManager
import com.example.humsafar.ui.LoginScreen
import com.example.humsafar.ui.SignUpScreen
import com.example.humsafar.ui.MapScreen
import com.example.humsafar.ui.HeritageDetailScreen
import com.example.humsafar.ui.NodeDetailScreen
import com.example.humsafar.ui.ProfileScreen
import com.example.humsafar.ui.VoiceChatScreen
import com.example.humsafar.ui.QrScanScreen

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
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToQrScan  = { siteId ->
                    navController.navigate(qrScanRoute("Site", siteId.toString()))
                }
            )
        }

        // ── Heritage site overview ────────────────────────────────────────
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
                siteName           = siteName,
                siteId             = siteId,
                onBack             = { navController.popBackStack() },
                onNavigateToVoice  = { name, id -> navController.navigate(voiceChatRoute(name, id)) },
                onNavigateToQrScan = { id -> navController.navigate(qrScanRoute("Site", id.toString())) }
            )
        }

        // ── Node detail — nodeId + siteId are both Ints ───────────────────
        composable(
            route     = "node/{nodeId}/{siteId}/{isKing}",
            arguments = listOf(
                navArgument("nodeId") { type = NavType.IntType },
                navArgument("siteId") { type = NavType.IntType },
                navArgument("isKing") { type = NavType.BoolType }
            )
        ) { backStack ->
            val nodeId = backStack.arguments?.getInt("nodeId") ?: 0
            val siteId = backStack.arguments?.getInt("siteId") ?: 0
            val isKing = backStack.arguments?.getBoolean("isKing") ?: false

            NodeDetailScreen(
                nodeId            = nodeId,
                siteId            = siteId,
                isKing            = isKing,
                onBack            = { navController.popBackStack() },
                onNavigateToQr    = { id -> navController.navigate(qrScanRoute("Site", id.toString())) },
                onNavigateToVoice = { name, id -> navController.navigate(voiceChatRoute(name, id)) }
            )
        }

        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

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

        // ── QR Scanner ────────────────────────────────────────────────────
        composable(
            route     = "qr/{siteName}/{siteId}",
            arguments = listOf(
                navArgument("siteName") { type = NavType.StringType },
                navArgument("siteId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val siteName   = backStack.arguments?.getString("siteName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            val siteId     = backStack.arguments?.getString("siteId") ?: ""
            val trip       = TripManager.state.collectAsState()

            QrScanScreen(
                monumentId   = siteId.toLongOrNull() ?: 0L,   // kept for compat
                currentLat   = trip.value.lastLat,
                currentLng   = trip.value.lastLng,
                onNodeReady  = { nodeId, _, isKing, scanSiteId ->
                    navController.navigate(nodeDetailRoute(nodeId, scanSiteId, isKing)) {
                        popUpTo("qr/$siteName/$siteId") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ── Route helpers ─────────────────────────────────────────────────────────────

fun heritageDetailRoute(siteName: String, siteId: String): String {
    val encoded = java.net.URLEncoder.encode(siteName, "UTF-8")
    return "detail/$encoded/$siteId"
}

fun voiceChatRoute(siteName: String, siteId: String): String {
    val encoded = java.net.URLEncoder.encode(siteName, "UTF-8")
    return "voice/$encoded/$siteId"
}

fun qrScanRoute(siteName: String, siteId: String): String {
    val encoded = java.net.URLEncoder.encode(siteName, "UTF-8")
    return "qr/$encoded/$siteId"
}

fun nodeDetailRoute(nodeId: Int, siteId: Int, isKing: Boolean): String =
    "node/$nodeId/$siteId/$isKing"