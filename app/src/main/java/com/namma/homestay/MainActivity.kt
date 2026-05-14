package com.namma.homestay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import coil.Coil
import coil.ImageLoader
import com.namma.homestay.ui.DataUriFetcher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import com.namma.homestay.ui.theme.NammaHomestayTheme
import com.namma.homestay.ui.theme.KarGreen
import com.namma.homestay.ui.theme.KarGreenLight
import com.namma.homestay.ui.screens.DashboardScreen
import com.namma.homestay.ui.screens.RoleSelectionScreen
import com.namma.homestay.ui.screens.LoginScreen
import com.namma.homestay.ui.screens.WelcomeScreen
import com.namma.homestay.ui.screens.ListingDetailScreen
import com.namma.homestay.ui.screens.ChatRoomScreen
import com.namma.homestay.ui.screens.MenuUpdateScreen
import com.namma.homestay.ui.screens.ManageRoomsScreen
import com.namma.homestay.ui.screens.TravelerHomeScreen
import com.namma.homestay.ui.screens.WishlistScreen
import com.namma.homestay.ui.screens.SettingsScreen
import com.namma.homestay.ui.screens.InquiriesScreen
import com.namma.homestay.ui.screens.AddEditListingScreen
import com.namma.homestay.ui.screens.AIFoodAnalysisScreen
import com.namma.homestay.ui.screens.BlockDatesScreen
import com.namma.homestay.ui.screens.ChecklistScreen
import com.namma.homestay.ui.screens.MenuGatewayScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Coil to support base64 data: URIs (images stored in Firestore)
        Coil.setImageLoader(
            ImageLoader.Builder(applicationContext)
                .components { add(DataUriFetcher.Factory()) }
                .build()
        )
        setContent {
            NammaHomestayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val auth = Firebase.auth
    var userRole by remember { mutableStateOf<String?>(null) }

    // Fetch user role whenever the user changes or we app starts
    LaunchedEffect(auth.currentUser) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                // Use withTimeoutOrNull to prevent hanging if Firestore is not responsive
                val doc = withTimeoutOrNull(15000) {
                    Firebase.firestore.collection("users").document(uid).get().await()
                }
                if (doc != null && doc.exists()) {
                    userRole = doc.getString("role")
                    Log.d("MainActivity", "Role loaded: $userRole")
                } else {
                    userRole = null
                    Log.d("MainActivity", "User doc does not exist or timed out")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching user role", e)
                userRole = null
            }
        } else {
            userRole = null
        }
    }

    // Determine if we should show host-specific items
    // Priority: 
    // 1. userRole from Firestore (Source of truth)
    // 2. Initial role from login/dashboard route if userRole hasn't loaded yet
    val isHost = when {
        userRole != null -> userRole == "host"
        currentRoute == "dashboard" || currentRoute?.startsWith("inquiriesHost") == true || currentRoute == "menuGateway" -> true
        currentRoute == "travelerHome" || currentRoute == "wishlist" || currentRoute?.startsWith("inquiriesTraveler") == true -> false
        else -> false // Default/Welcome
    }

    Scaffold(
        bottomBar = {
            val showBottomNav = currentRoute in listOf("dashboard", "travelerHome", "wishlist", "inquiriesHost", "inquiriesTraveler", "settings", "menuGateway")
            if (showBottomNav) {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 10.sp) },
                        selected = currentRoute == "dashboard" || currentRoute == "travelerHome",
                        onClick = { 
                            val dest = if (isHost) "dashboard" else "travelerHome"
                            navController.navigate(dest) { 
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            } 
                        },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = KarGreen, selectedTextColor = KarGreen, indicatorColor = KarGreenLight)
                    )
                    
                    if (!isHost) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Wishlist") },
                            label = { Text("Saved", fontSize = 10.sp) },
                            selected = currentRoute == "wishlist",
                            onClick = { 
                                navController.navigate("wishlist") { 
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } 
                            },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = KarGreen, selectedTextColor = KarGreen, indicatorColor = KarGreenLight)
                        )
                    }
                    
                    if (isHost) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = "Menu") },
                            label = { Text("Menu", fontSize = 10.sp) },
                            selected = currentRoute == "menuGateway",
                            onClick = { 
                                navController.navigate("menuGateway") { 
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                } 
                            },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = KarGreen, selectedTextColor = KarGreen, indicatorColor = KarGreenLight)
                        )
                    }
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Message, contentDescription = "Messages") },
                        label = { Text("Inbox", fontSize = 10.sp) },
                        selected = currentRoute == "inquiriesHost" || currentRoute == "inquiriesTraveler",
                        onClick = { 
                            val dest = if (isHost) "inquiriesHost" else "inquiriesTraveler"
                            navController.navigate(dest) { 
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            } 
                        },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = KarGreen, selectedTextColor = KarGreen, indicatorColor = KarGreenLight)
                    )
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Profile", fontSize = 10.sp) },
                        selected = currentRoute == "settings",
                        onClick = { 
                            navController.navigate("settings") { 
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            } 
                        },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = KarGreen, selectedTextColor = KarGreen, indicatorColor = KarGreenLight)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = "welcome", modifier = Modifier.padding(innerPadding)) {
            composable("welcome") { 
                WelcomeScreen(navController) 
            }
            composable("roleSelection") { RoleSelectionScreen(navController) }
            composable("login/{role}") { backStackEntry ->
                val role = backStackEntry.arguments?.getString("role") ?: "traveler"
                LoginScreen(navController, role)
            }
            composable("dashboard") { 
                if (auth.currentUser == null) {
                    navController.navigate("welcome") { popUpTo(0) }
                } else {
                    DashboardScreen(navController)
                }
            }
            composable("travelerHome") { 
                if (auth.currentUser == null) {
                    navController.navigate("welcome") { popUpTo(0) }
                } else {
                    TravelerHomeScreen(navController)
                }
            }
            composable("wishlist") { WishlistScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
            composable("inquiriesHost") { InquiriesScreen(navController, isHost = true) }
            composable("inquiriesTraveler") { InquiriesScreen(navController, isHost = false) }
            composable("menuGateway") { MenuGatewayScreen(navController) }
            
            composable("addListing") { AddEditListingScreen(navController, null) }
            composable("editListing/{listingId}") { backStackEntry ->
                val listingId = backStackEntry.arguments?.getString("listingId")
                AddEditListingScreen(navController, listingId)
            }
            composable("checklist") { ChecklistScreen(navController) }
            composable("blockDates/{listingId}") { backStackEntry ->
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                BlockDatesScreen(navController, listingId)
            }
            
            composable("listingDetail/{listingId}") { backStackEntry ->
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                ListingDetailScreen(navController, listingId)
            }
            
            composable("chatRoom/{inquiryId}") { backStackEntry ->
                val inquiryId = backStackEntry.arguments?.getString("inquiryId") ?: ""
                ChatRoomScreen(navController, inquiryId)
            }
            
            composable("menuUpdate/{listingId}") { backStackEntry ->
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                MenuUpdateScreen(navController, listingId)
            }
            composable("manageRooms/{listingId}") { backStackEntry ->
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                ManageRoomsScreen(navController, listingId)
            }
            composable("aiFoodAnalysis/{listingId}") { backStackEntry ->
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                AIFoodAnalysisScreen(navController, listingId)
            }
        }
    }
}
