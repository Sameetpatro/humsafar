// app/src/main/java/com/example/humsafar/navigation/AppNavigation.kt

package com.example.humsafar.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.data.TripManager
import com.example.humsafar.ui.*
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ── Auth-aware start destination ──────────────────────────────────────────
    // If user is already logged in (email, google, or anonymous), skip login screen
    val currentUser by AuthManager.currentUser.collectAsState()
    val startDest = if (currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDest) {

        composable("login") {
            LoginScreen(
                onSignupClick  = { navController.navigate("signup") },
                onBypassClick  = {
                    // Guest = anonymous Firebase sign-in, handled inside LoginScreen
                    // This callback fires after anonymous auth succeeds
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                onLoginClick    = { navController.popBackStack() },
                onBypassClick   = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            MapScreen(
                onNavigateToVoice    = { name, id -> navController.navigate(voiceChatRoute(name, id, "")) },
                onNavigateToDetail   = { name, id -> navController.navigate(heritageDetailRoute(name, id)) },
                onNavigateToProfile  = { navController.navigate("profile") },
                onNavigateToQrScan   = { siteId ->
                    navController.navigate(qrScanRoute("Site", siteId.toString()))
                },
                onNavigateToSiteInfo = { siteId, siteName ->
                    navController.navigate(siteInfoRoute(siteId, siteName))
                }
            )
        }

        composable(
            route = "site_info/{siteId}/{siteName}",
            arguments = listOf(
                navArgument("siteId")   { type = NavType.IntType },
                navArgument("siteName") { type = NavType.StringType }
            )
        ) { backStack ->
            val siteId   = backStack.arguments?.getInt("siteId") ?: 0
            val siteName = backStack.arguments?.getString("siteName")
                ?.let { URLDecoder.decode(it, "UTF-8") } ?: "Heritage Site"
            val site = com.example.humsafar.data.HeritageRepository.sites
                .find { it.id == siteId.toString() }
            if (site != null) {
                SiteInfoScreen(
                    siteId    = siteId,
                    siteName  = siteName,
                    latitude  = site.latitude,
                    longitude = site.longitude,
                    onBack    = { navController.popBackStack() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Site not found")
                }
            }
        }

        composable(
            route     = "detail/{siteName}/{siteId}",
            arguments = listOf(
                navArgument("siteName") { type = NavType.StringType },
                navArgument("siteId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val siteName = backStack.arguments?.getString("siteName")
                ?.let { URLDecoder.decode(it, "UTF-8") } ?: "Heritage Site"
            val siteId   = backStack.arguments?.getString("siteId") ?: ""
            HeritageDetailScreen(
                siteName           = siteName,
                siteId             = siteId,
                onBack             = { navController.popBackStack() },
                onNavigateToVoice  = { name, id -> navController.navigate(voiceChatRoute(name, id, "")) },
                onNavigateToQrScan = { id -> navController.navigate(qrScanRoute("Site", id.toString())) }
            )
        }

        // ── Node detail ───────────────────────────────────────────────────
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
                nodeId                 = nodeId,
                siteId                 = siteId,
                isKing                 = isKing,
                onBack                 = { navController.popBackStack() },
                onNavigateToQr         = { id: Long -> navController.navigate(qrScanRoute("Site", id.toString())) },
                onNavigateToVoice      = { name: String, _: String ->
                    navController.navigate(voiceChatRoute(name, siteId.toString(), nodeId.toString()))
                },
                onNavigateToDirections = { dirSiteId: Int, dirSiteName: String ->
                    navController.navigate(directionsRoute(dirSiteId, dirSiteName))
                },
                onNavigateToReview     = { tripId: Int, cSiteId: Int, cSiteName: String, visited: Int, total: Int ->
                    navController.navigate(reviewRoute(tripId, cSiteId, cSiteName, visited, total)) {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        // ── Directions screen ─────────────────────────────────────────────
        composable(
            route     = "directions/{siteId}/{siteName}",
            arguments = listOf(
                navArgument("siteId")   { type = NavType.IntType },
                navArgument("siteName") { type = NavType.StringType }
            )
        ) { backStack ->
            val siteId   = backStack.arguments?.getInt("siteId") ?: 0
            val siteName = backStack.arguments?.getString("siteName")
                ?.let { URLDecoder.decode(it, "UTF-8") } ?: "Heritage Site"
            DirectionsScreen(
                siteId   = siteId,
                siteName = siteName,
                onBack   = { navController.popBackStack() }
            )
        }

        // ── Review screen ─────────────────────────────────────────────────
        composable(
            route     = "review/{tripId}/{siteId}/{siteName}/{visitedCount}/{totalCount}",
            arguments = listOf(
                navArgument("tripId")       { type = NavType.IntType },
                navArgument("siteId")       { type = NavType.IntType },
                navArgument("siteName")     { type = NavType.StringType },
                navArgument("visitedCount") { type = NavType.IntType },
                navArgument("totalCount")   { type = NavType.IntType }
            )
        ) { backStack ->
            val tripId       = backStack.arguments?.getInt("tripId") ?: 0
            val siteId       = backStack.arguments?.getInt("siteId") ?: 0
            val siteName     = backStack.arguments?.getString("siteName")
                ?.let { URLDecoder.decode(it, "UTF-8") } ?: "Heritage Site"
            val visitedCount = backStack.arguments?.getInt("visitedCount") ?: 0
            val totalCount   = backStack.arguments?.getInt("totalCount") ?: 0
            ReviewScreen(
                tripId                     = tripId,
                siteId                     = siteId,
                siteName                   = siteName,
                visitedCount               = visitedCount,
                totalCount                 = totalCount,
                onNavigateToTripCompletion = {
                    navController.navigate(tripCompletionRoute(siteId, siteName, visitedCount, totalCount)) {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onSkip = {
                    navController.navigate(tripCompletionRoute(siteId, siteName, visitedCount, totalCount)) {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        // ── Trip completion screen ────────────────────────────────────────
        composable(
            route     = "trip_completion/{siteId}/{siteName}/{visitedCount}/{totalCount}",
            arguments = listOf(
                navArgument("siteId")       { type = NavType.IntType },
                navArgument("siteName")     { type = NavType.StringType },
                navArgument("visitedCount") { type = NavType.IntType },
                navArgument("totalCount")   { type = NavType.IntType }
            )
        ) { backStack ->
            val siteId       = backStack.arguments?.getInt("siteId") ?: 0
            val siteName     = backStack.arguments?.getString("siteName")
                ?.let { URLDecoder.decode(it, "UTF-8") } ?: "Heritage Site"
            val visitedCount = backStack.arguments?.getInt("visitedCount") ?: 0
            val totalCount   = backStack.arguments?.getInt("totalCount") ?: 0
            TripCompletionScreen(
                siteId                   = siteId,
                siteName                 = siteName,
                visitedNodesCount        = visitedCount,
                totalNodesCount          = totalCount,
                onExploreRecommendations = {
                    TripManager.clear()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onSkip = {
                    TripManager.clear()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        // ── Profile — with sign out ────────────────────────────────────────
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    AuthManager.signOut()
                    navController.navigate("login") {
                        // Clear the ENTIRE back stack — no destination to go back to
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Voice ─────────────────────────────────────────────────────────
        composable(
            route     = "voice/{siteName}/{siteId}/{nodeId}",
            arguments = listOf(
                navArgument("siteName") { type = NavType.StringType },
                navArgument("siteId")   { type = NavType.StringType },
                navArgument("nodeId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val siteName = backStack.arguments?.getString("siteName")
                ?.let { URLDecoder.decode(it, "UTF-8") } ?: "Heritage Site"
            val siteId   = backStack.arguments?.getString("siteId") ?: ""
            val nodeId   = backStack.arguments?.getString("nodeId") ?: ""
            VoiceChatScreen(
                siteName             = siteName,
                siteId               = siteId,
                nodeId               = nodeId,
                onBack               = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("profile") }
            )
        }

        // ── QR Scan ───────────────────────────────────────────────────────
        composable(
            route     = "qr/{siteName}/{siteId}",
            arguments = listOf(
                navArgument("siteName") { type = NavType.StringType },
                navArgument("siteId")   { type = NavType.StringType }
            )
        ) { backStack ->
            val siteName = backStack.arguments?.getString("siteName")
                ?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
            val siteId   = backStack.arguments?.getString("siteId") ?: ""
            val trip     = TripManager.state.collectAsState()
            QrScanScreen(
                monumentId  = siteId.toLongOrNull() ?: 0L,
                currentLat  = trip.value.lastLat,
                currentLng  = trip.value.lastLng,
                onNodeReady = { nodeId, _, isKing, scanSiteId ->
                    navController.navigate(nodeDetailRoute(nodeId, scanSiteId, isKing)) {
                        popUpTo("home") { inclusive = false }  // clears QR + any previous nodes from stack
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ── Route helpers ─────────────────────────────────────────────────────────────

fun heritageDetailRoute(siteName: String, siteId: String): String {
    val encoded = URLEncoder.encode(siteName, "UTF-8")
    return "detail/$encoded/$siteId"
}

fun voiceChatRoute(siteName: String, siteId: String, nodeId: String): String {
    val encoded = URLEncoder.encode(siteName, "UTF-8")
    return "voice/$encoded/$siteId/$nodeId"
}

fun qrScanRoute(siteName: String, siteId: String): String {
    val encoded = URLEncoder.encode(siteName, "UTF-8")
    return "qr/$encoded/$siteId"
}

fun nodeDetailRoute(nodeId: Int, siteId: Int, isKing: Boolean): String =
    "node/$nodeId/$siteId/$isKing"

fun directionsRoute(siteId: Int, siteName: String): String {
    val encoded = URLEncoder.encode(siteName, "UTF-8")
    return "directions/$siteId/$encoded"
}

fun tripCompletionRoute(siteId: Int, siteName: String, visitedCount: Int, totalCount: Int): String {
    val encoded = URLEncoder.encode(siteName, "UTF-8")
    return "trip_completion/$siteId/$encoded/$visitedCount/$totalCount"
}

fun siteInfoRoute(siteId: Int, siteName: String): String {
    val encoded = URLEncoder.encode(siteName, "UTF-8")
    return "site_info/$siteId/$encoded"
}

fun reviewRoute(tripId: Int, siteId: Int, siteName: String, visitedCount: Int, totalCount: Int): String {
    val encoded = URLEncoder.encode(siteName, "UTF-8")
    return "review/$tripId/$siteId/$encoded/$visitedCount/$totalCount"
}