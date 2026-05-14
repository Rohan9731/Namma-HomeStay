package com.namma.homestay.ui.screens

import android.util.Log
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.namma.homestay.models.Listing
import com.namma.homestay.models.ListingImage
import com.namma.homestay.models.Room
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRoomsScreen(navController: NavController, listingId: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var listing by remember { mutableStateOf<Listing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var editingRoom by remember { mutableStateOf<Room?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listingId) {
        val fetchedListing = FirebaseRepository.getListing(listingId)
        listing = fetchedListing
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Rooms Management", fontSize = 12.sp, color = KarOrange, fontWeight = FontWeight.Bold)
                        Text(listing?.name ?: "Loading...", fontSize = 20.sp, color = KarGreenDark)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingRoom = Room(id = java.util.UUID.randomUUID().toString().substring(0, 8), price = listing?.price ?: 0.0)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Room", tint = KarGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KarGreen)
            }
        } else if (listing == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Listing not found", color = Color.Red)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp)
            ) {
                val rooms = listing?.rooms ?: emptyList()
                if (rooms.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No rooms added yet. Tap + to add.", color = Color.Gray)
                        }
                    }
                } else {
                    items(rooms) { room ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Row(modifier = Modifier.height(120.dp)) {
                                AsyncImage(
                                    model = room.images.firstOrNull()?.url ?: room.washroomImages.firstOrNull()?.url ?: "https://images.unsplash.com/photo-1590490360182-c33d57733427",
                                    contentDescription = "Room",
                                    modifier = Modifier.width(120.dp).fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                                Column(
                                    modifier = Modifier.weight(1f).padding(12.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("₹${room.price}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KarGreen)
                                        Box(
                                            modifier = Modifier.background(
                                                if (room.status == "occupied") KarOrange else KarGreen,
                                                RoundedCornerShape(4.dp)
                                            ).padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (room.status == "occupied") "OCCUPIED" else "AVAILABLE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(room.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (room.type == "bedroom_attached") Icons.Default.Bathtub else Icons.Default.Bed, contentDescription = "", modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${room.capacity} Guests • ${if (room.type == "bedroom_attached") "Attached" else "Shared"}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        IconButton(onClick = { editingRoom = room }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        FirebaseRepository.deleteRoom(listingId, room.id)
                                                        // Refresh listing
                                                        listing = FirebaseRepository.getListing(listingId)
                                                    } catch (e: Exception) {
                                                        Log.e("ManageRooms", "Delete failed", e)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val roomImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            val currentRoomId = editingRoom?.id ?: return@let
            scope.launch {
                val path = "rooms/${System.currentTimeMillis()}.jpg"
                FirebaseRepository.uploadImage(context, imageUri, path)
                    .onSuccess { url ->
                        if (editingRoom?.id == currentRoomId) {
                            editingRoom = editingRoom?.copy(
                                images = (editingRoom?.images ?: emptyList()) + ListingImage(url = url)
                            )
                        }
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar("Failed to upload room image")
                    }
            }
        }
    }

    if (editingRoom != null) {
        val currentRoom = editingRoom!!
        key(currentRoom.id) {
            var roomName by remember { mutableStateOf(currentRoom.name) }
            var roomCapacity by remember { mutableStateOf(currentRoom.capacity.toString()) }
            var roomPrice by remember { mutableStateOf(currentRoom.price.toString()) }
            var roomType by remember { mutableStateOf(currentRoom.type) }

            AlertDialog(
                onDismissRequest = { editingRoom = null },
                title = { Text(if (listing?.rooms?.any { it.id == currentRoom.id } == true) "Edit Room" else "Add Room") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                        OutlinedTextField(
                            value = roomName,
                            onValueChange = { roomName = it },
                            label = { Text("Room Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = roomCapacity,
                                onValueChange = { roomCapacity = it },
                                label = { Text("Capacity") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = roomPrice,
                                onValueChange = { roomPrice = it },
                                label = { Text("Price") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Room Type", fontSize = 12.sp, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { roomType = "bedroom_attached" },
                                colors = ButtonDefaults.buttonColors(containerColor = if (roomType == "bedroom_attached") KarGreen else Color.LightGray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Attached", fontSize = 10.sp)
                            }
                            Button(
                                onClick = { roomType = "bedroom_shared" },
                                colors = ButtonDefaults.buttonColors(containerColor = if (roomType == "bedroom_shared") KarGreen else Color.LightGray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Shared", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Room Images", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(currentRoom.images) { img ->
                                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))) {
                                    AsyncImage(
                                        model = img.url,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            editingRoom = editingRoom?.copy(
                                                images = editingRoom?.images?.filter { it.url != img.url } ?: emptyList()
                                            )
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.Black.copy(0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray.copy(0.3f))
                                        .clickable { roomImagePicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add", tint = Color.Gray)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val finalRoom = currentRoom.copy(
                                    name = roomName,
                                    capacity = roomCapacity.toIntOrNull() ?: 2,
                                    price = roomPrice.toDoubleOrNull() ?: 0.0,
                                    type = roomType
                                )
                                val result = if (listing?.rooms?.any { it.id == finalRoom.id } == true) {
                                    FirebaseRepository.updateRoom(listingId, finalRoom)
                                } else {
                                    FirebaseRepository.addRoom(listingId, finalRoom)
                                }
                                
                                if (result.isSuccess) {
                                    listing = FirebaseRepository.getListing(listingId)
                                    editingRoom = null
                                    snackbarHostState.showSnackbar("Room saved")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to save room")
                                }
                                isSaving = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KarGreen)
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingRoom = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
