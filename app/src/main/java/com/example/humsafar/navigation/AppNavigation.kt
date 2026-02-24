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
                onNavigateToQrScan  = { monumentId ->
                    navController.navigate(qrScanRoute("Site", monumentId.toString()))
                }
            )
        }

        // ── Heritage site overview (static content + video) ───────────────
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

        // ── Node detail (Spring Boot data — loaded by nodeId Long) ────────
        composable(
            route     = "node/{nodeId}/{isKing}",
            arguments = listOf(
                navArgument("nodeId") { type = NavType.LongType },
                navArgument("isKing") { type = NavType.BoolType }
            )
        ) { backStack ->
            val nodeId = backStack.arguments?.getLong("nodeId") ?: 0L
            val isKing = backStack.arguments?.getBoolean("isKing") ?: false

            NodeDetailScreen(
                nodeId  = nodeId,
                isKing  = isKing,
                onBack  = { navController.popBackStack() },
                onNavigateToQr = { monumentId ->
                    navController.navigate(qrScanRoute("Site", monumentId.toString()))
                },
                onNavigateToVoice = { name, id ->
                    navController.navigate(voiceChatRoute(name, id))
                }
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
            val monumentId = siteId.toLongOrNull() ?: 0L
            val trip       = TripManager.state.collectAsState()

            QrScanScreen(
                monumentId  = monumentId,
                currentLat  = trip.value.lastLat,
                currentLng  = trip.value.lastLng,
                onNodeReady = { nodeId, _, isKing ->
                    // Navigate to NodeDetailScreen (Spring Boot data), not HeritageDetailScreen
                    navController.navigate("node/$nodeId/$isKing") {
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

fun nodeDetailRoute(nodeId: Long, isKing: Boolean): String = "node/$nodeId/$isKing"