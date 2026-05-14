package com.namma.homestay.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
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
import com.google.firebase.ktx.Firebase
import com.namma.homestay.models.*
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(navController: NavController, listingId: String) {
    var listing by remember { mutableStateOf<Listing?>(null) }
    var menu by remember { mutableStateOf<Menu?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRooms by remember { mutableStateOf(setOf<String>()) }
    var showInquiryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectingCheckIn by remember { mutableStateOf(true) }
    var checkInDate by remember { mutableStateOf("") }
    var checkOutDate by remember { mutableStateOf("") }
    var guests by remember { mutableStateOf("1") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var currentImageIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(listingId) {
        try {
            FirebaseRepository.getListing(listingId)?.let { fetchedListing ->
                listing = fetchedListing
                // Fetch menu for today
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdf.format(Date())
                FirebaseRepository.getMenu(listingId, todayStr)?.let { fetchedMenu ->
                    menu = fetchedMenu
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val totalPrice = selectedRooms.sumOf { roomId ->
        listing?.rooms?.find { it.id == roomId }?.price ?: 0.0
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KarGreen)
        }
        return
    }

    if (listing == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Listing not found")
        }
        return
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Estimate", fontSize = 12.sp, color = Color.Gray)
                            Text("₹$totalPrice", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KarOrange)
                        }
                        Button(
                            onClick = { showInquiryDialog = true },
                            enabled = selectedRooms.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = KarGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Reserve Stay")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            // Hero Image
            item {
                val displayImages = if (listing!!.images.isNotEmpty()) {
                    listOf(listing!!.coverPhoto) + listing!!.images.map { it.url }
                } else {
                    listOf(listing!!.coverPhoto.ifEmpty { "https://images.unsplash.com/photo-1598332896317-5181330cc5aa" })
                }

                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    AsyncImage(
                        model = displayImages.getOrNull(currentImageIndex) ?: listing!!.coverPhoto,
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Arrows for navigation
                    if (displayImages.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    currentImageIndex = if (currentImageIndex > 0) currentImageIndex - 1 else displayImages.size - 1
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                            }
                            IconButton(
                                onClick = { 
                                    currentImageIndex = (currentImageIndex + 1) % displayImages.size
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                            }
                        }

                        // Dots indicator
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            displayImages.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (index == currentImageIndex) Color.White else Color.White.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color(0x80FFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            }

            // Title & Subtitle
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = listing!!.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarGreenDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "", tint = KarOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = listing!!.location.address, fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            // DATE SELECTOR FIRST
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KarGreenLight.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Dates", fontWeight = FontWeight.Bold, color = KarGreenDark)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = checkInDate,
                                onValueChange = { },
                                label = { Text("Check-In", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).clickable {
                                    selectingCheckIn = true
                                    showDatePicker = true
                                },
                                enabled = false,
                                readOnly = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = KarGreen.copy(alpha = 0.5f),
                                    disabledLabelColor = KarGreen
                                )
                            )
                            OutlinedTextField(
                                value = checkOutDate,
                                onValueChange = { },
                                label = { Text("Check-Out", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).clickable {
                                    selectingCheckIn = false
                                    showDatePicker = true
                                },
                                enabled = false,
                                readOnly = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = KarGreen.copy(alpha = 0.5f),
                                    disabledLabelColor = KarGreen
                                )
                            )
                        }
                    }
                }
            }

            // About
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About the Home", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(listing!!.description, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
                }
            }

            // Amenities
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Amenities", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Simplified grid
                    listing!!.amenities.forEach { amenity ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "", tint = KarGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(amenity.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                    }
                }
            }

            // Menu
            if (menu != null && menu!!.dishes.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Fresh Menu", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        menu!!.dishes.forEach { dish ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = KarGreenLight.copy(alpha = 0.3f)),
                                border = borderStroke(KarGreen.copy(alpha = 0.1f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (dish.imageUrl != null) {
                                        AsyncImage(
                                            model = dish.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dish.name, fontWeight = FontWeight.Bold, color = KarGreenDark)
                                        Text(dish.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                                    }
                                    Text("₹${dish.price}", fontWeight = FontWeight.Bold, color = KarOrange)
                                }
                            }
                        }
                    }
                }
            }

            // Rooms
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Choose your Stay", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    listing!!.rooms.forEach { room ->
                        val isSelected = selectedRooms.contains(room.id)
                        val isAvailable = FirebaseRepository.isRoomAvailable(room, checkInDate, checkOutDate)
                        var showRoomPhotos by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) KarOrange else if (!isAvailable && checkInDate.isNotEmpty()) Color.Red.copy(alpha = 0.3f) else Color.LightGray,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    showRoomPhotos = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isAvailable && checkInDate.isNotEmpty()) Color(0xFFFEE2E2) 
                                                else Color.White
                            )
                        ) {
                            Column {
                                Box {
                                    AsyncImage(
                                        model = room.images.firstOrNull()?.url ?: "https://images.unsplash.com/photo-1590490360182-c33d57733427",
                                        contentDescription = "Room",
                                        modifier = Modifier.fillMaxWidth().height(150.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (!isAvailable && checkInDate.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.Center),
                                            color = Color.Red.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "OCCUPIED FOR SELECTED DATES",
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(room.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                                        Text("₹${room.price}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Capacity: ${room.capacity} Guests • ${room.type.replace("_", " ").uppercase()}", fontSize = 12.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (isAvailable && checkInDate.isNotEmpty()) {
                                                val newSet = selectedRooms.toMutableSet()
                                                if (isSelected) newSet.remove(room.id) else newSet.add(room.id)
                                                selectedRooms = newSet
                                            }
                                        },
                                        enabled = isAvailable && checkInDate.isNotEmpty() && checkOutDate.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) KarOrange else if (isAvailable && checkInDate.isNotEmpty()) KarGreen.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                                            contentColor = if (isSelected) Color.White else if (isAvailable && checkInDate.isNotEmpty()) KarGreen else Color.Gray
                                        )
                                    ) {
                                        Text(
                                            when {
                                                isSelected -> "Selected"
                                                checkInDate.isEmpty() -> "Select Dates First"
                                                !isAvailable -> "Unavailable"
                                                else -> "Select Room"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (showRoomPhotos) {
                            AlertDialog(
                                onDismissRequest = { showRoomPhotos = false },
                                title = { Text(room.name) },
                                text = {
                                    Column {
                                        val roomImages = room.images.map { it.url }
                                        if (roomImages.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                roomImages.forEach { url ->
                                                    AsyncImage(
                                                        model = url,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        } else {
                                            Text("No additional photos for this room.", color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(room.description.ifEmpty { "Comfortable room with ${room.amenities.joinToString(", ")}" }, fontSize = 14.sp)
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (isAvailable && checkInDate.isNotEmpty()) {
                                                val newSet = selectedRooms.toMutableSet()
                                                if (isSelected) newSet.remove(room.id) else newSet.add(room.id)
                                                selectedRooms = newSet
                                            }
                                            showRoomPhotos = false
                                        },
                                        enabled = isAvailable && checkInDate.isNotEmpty()
                                    ) {
                                        Text(if (isSelected) "Remove Selection" else "Select for Reservation")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRoomPhotos = false }) {
                                        Text("Close")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.format(Date(it))
                    } ?: ""
                    if (selectingCheckIn) {
                        checkInDate = date
                    } else {
                        checkOutDate = date
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showInquiryDialog) {
        AlertDialog(
            onDismissRequest = { showInquiryDialog = false },
            title = { Text("Stay Inquiry") },
            text = {
                Column {
                    OutlinedTextField(
                        value = checkInDate,
                        onValueChange = { },
                        label = { Text("Check-In Date") },
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectingCheckIn = true
                            showDatePicker = true
                        },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = checkOutDate,
                        onValueChange = { },
                        label = { Text("Check-Out Date") },
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectingCheckIn = false
                            showDatePicker = true
                        },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = guests,
                        onValueChange = { guests = it },
                        label = { Text("Guests Count") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message to Host") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSending,
                    onClick = {
                        isSending = true
                        val currentUser = FirebaseRepository.getCurrentUser()
                        if (currentUser == null) return@Button
                        
                        scope.launch {
                            val newInquiry = Inquiry(
                                travelerId = currentUser.uid,
                                travelerName = currentUser.displayName ?: "Guest",
                                hostId = listing!!.hostId,
                                listingId = listing!!.id,
                                listingName = listing!!.name,
                                listingImage = listing!!.coverPhoto,
                                checkIn = checkInDate,
                                checkOut = checkOutDate,
                                guests = guests.toIntOrNull() ?: 1,
                                message = message,
                                selectedRoomIds = selectedRooms.toList(),
                                totalPrice = totalPrice,
                                status = "sent",
                                read = false,
                                lastMessage = message
                            )
                            
                            val result = FirebaseRepository.createInquiry(newInquiry)
                            result.onSuccess { inquiryId ->
                                // Also add initial message
                                val initialChat = ChatMessage(
                                    senderId = currentUser.uid,
                                    senderRole = "traveler",
                                    text = "Initial Inquiry: $message",
                                    isAutoGenerated = true
                                )
                                FirebaseRepository.sendMessage(inquiryId, initialChat)
                                
                                val stayDetails = "STAY DETAILS:\nCheck-In: $checkInDate\nCheck-Out: $checkOutDate\nGuests: $guests\nRooms: ${selectedRooms.size}"
                                FirebaseRepository.sendMessage(inquiryId, ChatMessage(
                                    senderId = currentUser.uid,
                                    senderRole = "traveler",
                                    text = stayDetails,
                                    isAutoGenerated = true
                                ))

                                showInquiryDialog = false
                                navController.navigate("chatRoom/$inquiryId")
                            }.onFailure {
                                isSending = false
                                // TODO Show error
                            }
                        }
                    }
                ) {
                    Text(if (isSending) "Sending..." else "Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInquiryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
