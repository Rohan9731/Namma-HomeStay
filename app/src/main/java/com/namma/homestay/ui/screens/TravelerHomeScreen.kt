package com.namma.homestay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.namma.homestay.models.Listing
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelerHomeScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Home Food", "WiFi Ready", "Heritage", "Farm Stay", "Near Forest")

    var wishlist by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            FirebaseRepository.listenToAllListings().collect { allListings ->
                // Show listing if it has at least one available room (or no rooms configured yet)
                listings = allListings.filter { listing ->
                    listing.rooms.isEmpty() || listing.rooms.any { it.status == "available" }
                }
                isLoading = false
            }
        }
        
        val user = FirebaseRepository.getCurrentUser()
        if (user != null) {
            val wishlistListings = FirebaseRepository.getWishlist(user.uid)
            wishlist = wishlistListings.map { it.id }
        }
    }

    val filteredListings = listings.filter {
        (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.location.address.contains(searchQuery, ignoreCase = true))
        // Real app would filter by categories here if set up in DB
    }.sortedBy { it.price }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Find your escape",
                    style = Typography.titleLarge,
                    fontSize = 32.sp,
                    color = KarGreenDark
                )
                Text(
                    text = "AUTHENTIC KARNATAKA STAYS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    letterSpacing = 2.sp
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by location or name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        Box(modifier = Modifier.padding(8.dp).background(KarOrange, RoundedCornerShape(8.dp)).padding(8.dp)) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = KarGreen
                    )
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) KarGreen else Color.White, RoundedCornerShape(16.dp))
                                .border(borderStroke(if (isSelected) KarGreen else Color.LightGray), RoundedCornerShape(16.dp))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = category.uppercase(),
                                color = if (isSelected) Color.White else Color.DarkGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${filteredListings.size} EXPERIENCES FOUND", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
            }
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KarGreen)
                }
            }
        } else if (filteredListings.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No listings match your search.", color = Color.Gray)
                }
            }
        } else {
            items(filteredListings) { listing ->
                TravelerListingCard(navController, listing, wishlist.contains(listing.id)) {
                    scope.launch {
                        val isAdding = !wishlist.contains(listing.id)
                        val result = FirebaseRepository.toggleWishlist(listing.id, isAdding)
                        if (result.isSuccess) {
                            wishlist = if (isAdding) wishlist + listing.id else wishlist - listing.id
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TravelerListingCard(navController: NavController, listing: Listing, isWishlist: Boolean, onToggleWishlist: () -> Unit) {
    var currentImageIndex by remember(listing.id) { mutableIntStateOf(0) }
    val displayImages = remember(listing.id) {
        val all = mutableListOf<String>()
        val cover = listing.coverPhoto.ifEmpty { "https://images.unsplash.com/photo-1598332896317-5181330cc5aa" }
        all.add(cover)
        listing.images.forEach { img -> if (img.url.isNotEmpty()) all.add(img.url) }
        all
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("listingDetail/${listing.id}") },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                AsyncImage(
                    model = displayImages.getOrElse(currentImageIndex) { displayImages.first() },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Navigation arrows when multiple images exist
                if (displayImages.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                currentImageIndex = if (currentImageIndex > 0) currentImageIndex - 1 else displayImages.size - 1
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.White)
                        }
                        IconButton(
                            onClick = { currentImageIndex = (currentImageIndex + 1) % displayImages.size },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                        }
                    }

                    // Dot indicators
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        displayImages.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier.size(6.dp).background(
                                    if (index == currentImageIndex) Color.White else Color.White.copy(alpha = 0.5f),
                                    CircleShape
                                )
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("4.8", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                            .clickable { onToggleWishlist() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            if (isWishlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                            contentDescription = null, 
                            tint = if (isWishlist) Color.Red else Color.Gray, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(KarOrange, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("STARTING FROM", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("₹${listing.price}", style = Typography.titleLarge, fontSize = 20.sp, color = Color.White)
                    }
                }
            }
            
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(listing.name, style = Typography.titleLarge, fontSize = 24.sp, color = KarGreenDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.background(KarGreenLight, CircleShape).padding(4.dp)) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = KarGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = KarOrange, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(listing.location.address, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listing.amenities.take(3).forEach { amenity ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
                                .border(borderStroke(Color(0xFFE5E7EB)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(amenity.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(KarGreen, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VERIFIED STAY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = KarGreen)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("VIEW DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = KarOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = KarOrange, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
