package com.namma.homestay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.namma.homestay.models.Listing
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*

@Composable
fun MenuGatewayScreen(navController: NavController) {
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseRepository.getCurrentUser()
        if (currentUser != null) {
            Firebase.firestore.collection("listings")
                .whereEqualTo("hostId", currentUser.uid)
                .addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        val data = snap.documents.mapNotNull { it.toObject(Listing::class.java)?.copy(id = it.id) }
                        listings = data
                        
                        // If only one property, immediately redirect to its menu update screen
                        if (data.size == 1) {
                            navController.navigate("menuUpdate/${data[0].id}") {
                                popUpTo("menuGateway") { inclusive = true }
                            }
                        }
                    }
                    isLoading = false
                }
        } else {
            isLoading = false
        }
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
                        text = "SELECT PROPERTY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = KarOrange,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Manage Menus",
                        style = Typography.titleLarge,
                        fontSize = 32.sp,
                        color = KarGreenDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose a property to update today's kitchen menu.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KarGreen)
                    }
                }
            } else if (listings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFF8F9FA), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No properties yet", style = Typography.titleLarge, fontSize = 20.sp, color = KarGreenDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Please list your homestay first to manage its menu.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { navController.navigate("dashboard") },
                                colors = ButtonDefaults.buttonColors(containerColor = KarGreen)
                            ) {
                                Text("Go to Dashboard")
                            }
                        }
                    }
                }
            } else {
                items(listings) { listing ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("menuUpdate/${listing.id}") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = listing.coverPhoto.ifEmpty { "https://images.unsplash.com/photo-1598332896317-5181330cc5aa" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(listing.name, style = Typography.titleLarge, fontSize = 20.sp, color = KarGreenDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    listing.location.address,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF8F9FA), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
