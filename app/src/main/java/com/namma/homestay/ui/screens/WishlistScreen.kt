package com.namma.homestay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.namma.homestay.models.Listing
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WishlistScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val user = FirebaseRepository.getCurrentUser()
        if (user != null) {
            listings = FirebaseRepository.getWishlist(user.uid)
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "SAVED STAYS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = KarOrange,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Your Wishlist",
                        style = Typography.titleLarge,
                        fontSize = 32.sp,
                        color = KarGreenDark
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KarGreen)
                    }
                }
            } else if (listings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No saved stays yet.", color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Explore Karnataka and save your favorites.", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { navController.navigate("travelerHome") },
                                colors = ButtonDefaults.buttonColors(containerColor = KarGreen)
                            ) {
                                Text("Explore Now")
                            }
                        }
                    }
                }
            } else {
                items(listings.size) { index ->
                    val listing = listings[index]
                    TravelerListingCard(navController, listing, true) {
                        scope.launch {
                            val result = FirebaseRepository.toggleWishlist(listing.id, false)
                            if (result.isSuccess) {
                                listings = listings.filter { it.id != listing.id }
                            }
                        }
                    }
                }
            }
        }
    }
}
