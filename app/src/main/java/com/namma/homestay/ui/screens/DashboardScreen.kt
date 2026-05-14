package com.namma.homestay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun DashboardScreen(navController: NavController) {
    var stats by remember { mutableStateOf(DashboardStats(0, 0, 0, 0)) }
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Safety timeout for initial loading
        kotlinx.coroutines.delay(10000)
        if (isLoading) {
            isLoading = false // Stop showing spinner after 10s
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseRepository.getCurrentUser()
        if (currentUser != null) {
            FirebaseRepository.listenToHostListings(currentUser.uid).collect { data ->
                listings = data
                
                var totalRooms = 0
                var occupiedRooms = 0
                var emptyRooms = 0
                
                data.forEach { listing ->
                    listing.rooms.forEach { room ->
                        totalRooms++
                        if (room.status == "occupied" || room.blockedDates.isNotEmpty()) occupiedRooms++
                        else emptyRooms++
                    }
                }
                stats = DashboardStats(totalRooms, occupiedRooms, emptyRooms, data.size)
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Branding
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(KarGreen, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Namma Homestay",
                        style = Typography.titleLarge,
                        fontSize = 20.sp,
                        color = KarGreenDark
                    )
                    Text(
                        text = "AUTHENTIC HOSPITALITY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = KarOrange,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Header
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(
                        text = "HOST DASHBOARD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Manage your Home",
                        style = Typography.titleLarge,
                        fontSize = 32.sp,
                        color = KarGreenDark
                    )
                }
                IconButton(
                    onClick = { navController.navigate("addListing") },
                    modifier = Modifier.background(KarGreen, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Listing", tint = Color.White)
                }
            }
        }

        // Stats Cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(icon = Icons.Default.Bed, label = "Total Rooms", value = stats.rooms.toString(), color = KarGreen)
                    StatCard(icon = Icons.Default.Bolt, label = "Availability", value = stats.empty.toString(), color = KarGreenDark)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(icon = Icons.Default.AutoGraph, label = "Occupied", value = stats.occupied.toString(), color = KarOrange)
                    StatCard(icon = Icons.Default.Home, label = "Properties", value = stats.properties.toString(), color = Color(0xFF334155))
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("checklist") },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = borderStroke(Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(KarOrange.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = KarOrange, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Stay Ready Checklist", style = Typography.titleLarge, fontSize = 20.sp, color = KarGreenDark)
                            Text("PRE-ARRIVAL VERIFICATION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Your Properties",
                    style = Typography.titleLarge,
                    fontSize = 24.sp,
                    color = KarGreenDark
                )
                Text("${listings.size} ACTIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("addListing") },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(80.dp).background(Color(0xFFF8F9FA), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "You haven't listed any homestays yet, click here to add one.",
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(listings) { listing ->
                ListingHostCard(navController, listing, scope)
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun ListingHostCard(navController: NavController, listing: Listing, scope: kotlinx.coroutines.CoroutineScope) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                AsyncImage(
                    model = listing.coverPhoto.ifEmpty { "https://images.unsplash.com/photo-1598332896317-5181330cc5aa" },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val totalRooms = listing.rooms.size
                    val availableRooms = listing.rooms.count { it.status != "occupied" && it.blockedDates.isEmpty() }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(if (availableRooms > 0) KarGreen else Color.Gray, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$availableRooms/$totalRooms ROOMS AVAILABLE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(listing.name, style = Typography.titleLarge, fontSize = 24.sp, color = KarGreenDark)
                        Text(listing.location.address, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${listing.price}", style = Typography.titleLarge, fontSize = 20.sp)
                        Text("PER DAY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(icon = Icons.Default.DateRange, label = "Calendar", onClick = { navController.navigate("blockDates/${listing.id}") }, modifier = Modifier.weight(1f))
                    QuickActionButton(icon = Icons.Default.Bed, label = "Rooms", onClick = { navController.navigate("manageRooms/${listing.id}") }, modifier = Modifier.weight(1f))
                    QuickActionButton(icon = Icons.Default.RestaurantMenu, label = "Menu", onClick = { navController.navigate("menuUpdate/${listing.id}") }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { navController.navigate("editListing/${listing.id}") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.DarkGray),
                        border = borderStroke(Color.LightGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EDIT DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                FirebaseRepository.deleteListing(listing.id)
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("REMOVE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                if (listing.rooms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("CONFIGURED ROOMS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        TextButton(onClick = { navController.navigate("manageRooms/${listing.id}") }) {
                            Text("MANAGE ALL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = KarOrange)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listing.rooms.take(3).forEach { room ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .border(borderStroke(Color(0xFFE5E7EB)), RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { navController.navigate("manageRooms/${listing.id}") }
                            ) {
                                AsyncImage(
                                    model = room.images.firstOrNull()?.url ?: "https://images.unsplash.com/photo-1590490360182-c33d57733427",
                                    contentDescription = null,
                                    modifier = Modifier.width(80.dp).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.padding(12.dp).weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("₹${room.price}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = KarGreen)
                                        val isOccupied = room.status == "occupied" || room.blockedDates.isNotEmpty()
                                        Box(modifier = Modifier.background(if (isOccupied) KarOrange else KarGreen, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(if (isOccupied) "OCCUPIED" else "AVAILABLE", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(room.name, style = Typography.titleLarge, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                        Text(" ${room.capacity}", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(if (room.type == "bedroom_attached") Icons.Default.Bathtub else Icons.Default.Bed, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                        Text(if (room.type == "bedroom_attached") " ATTACHED" else " SHARED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxHeight().padding(end = 12.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFFF8F9FA), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.DarkGray)
    }
}



@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier.height(130.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = Typography.titleLarge,
                fontSize = 28.sp,
                color = Color(0xFF1E293B)
            )
            Text(
                text = label.uppercase(),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
        }
    }
}

data class DashboardStats(
    val rooms: Int,
    val occupied: Int,
    val empty: Int,
    val properties: Int
)

