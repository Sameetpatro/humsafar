package com.example.humsafar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.humsafar.ui.LoginScreen
import com.example.humsafar.ui.SignUpScreen
import com.example.humsafar.ui.HomeScreen
import com.example.humsafar.ui.MapScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        composable("login") {
            LoginScreen(
                onSignupClick = {
                    navController.navigate("signup")
                },
                onBypassClick = {
                    navController.navigate("home")
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                onLoginClick = {
                    navController.popBackStack()
                },
                onBypassClick = {
                    navController.navigate("home")
                }
            )
        }

        composable("home") {
            MapScreen()
        }
    }
}
