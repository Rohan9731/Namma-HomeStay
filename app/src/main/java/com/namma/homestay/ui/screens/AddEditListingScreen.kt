package com.namma.homestay.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.namma.homestay.ai.GeminiService
import com.namma.homestay.models.*
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun AddEditListingScreen(navController: NavController, listingId: String?) {
    var currentStep by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amenities by remember { mutableStateOf(setOf<String>()) }
    var attractions by remember { mutableStateOf(listOf<Attraction>()) }
    var isGeneratingDescription by remember { mutableStateOf(false) }
    var originalListing by remember { mutableStateOf<Listing?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var coverPhotoUrl by remember { mutableStateOf("") }
    var localCoverUri by remember { mutableStateOf<Uri?>(null) }
    var galleryImages by remember { mutableStateOf(listOf<ListingImage>()) }
    var localGalleryUris by remember { mutableStateOf(listOf<Uri>()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val fusedLocationClient = remember {
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Permission granted, detect location
            scope.launch {
                try {
                    val loc = fusedLocationClient.lastLocation.await()
                    loc?.let {
                        // Reverse geocoding via Nominatim with proper headers
                        val urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=${it.latitude}&lon=${it.longitude}"
                        val displayName = withContext(Dispatchers.IO) {
                            val url = java.net.URL(urlString)
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.setRequestProperty("User-Agent", "NammaHomestay/1.0")
                            connection.connect()
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = com.google.gson.JsonParser.parseString(response).asJsonObject
                            json.get("display_name").asString
                        }
                        address = displayName
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Location", "Failed to detect location", e)
                }
            }
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            localCoverUri = it
            scope.launch {
                val path = "listings/${System.currentTimeMillis()}.jpg"
                FirebaseRepository.uploadImage(context, it, path)
                    .onSuccess { url ->
                        coverPhotoUrl = url
                    }
                    .onFailure {
                        localCoverUri = null
                        snackbarHostState.showSnackbar("Failed to upload cover photo")
                    }
            }
        }
    }

    LaunchedEffect(listingId) {
        if (listingId != null) {
            FirebaseRepository.getListing(listingId)?.let { listing ->
                originalListing = listing
                name = listing.name
                address = listing.location.address
                price = if (listing.price > 0) listing.price.toString() else ""
                description = listing.description
                amenities = listing.amenities.toSet()
                attractions = listing.localAttractions
                coverPhotoUrl = listing.coverPhoto
                galleryImages = listing.images
            }
        }
    }

    val steps = listOf("Basic Info", "Photos", "Amenities", "Details", "Safety", "Spots")

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back", color = Color.DarkGray)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = Color.DarkGray)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else if (!isSaving) {
                                isSaving = true
                                scope.launch {
                                    try {
                                        val listing = if (listingId == null) {
                                            Listing(
                                                id = "",
                                                name = name,
                                                description = description,
                                                price = price.toDoubleOrNull() ?: 0.0,
                                                location = Location(address = address),
                                                amenities = amenities.toList(),
                                                localAttractions = attractions,
                                                coverPhoto = coverPhotoUrl,
                                                images = galleryImages,
                                                occupancyStatus = "available"
                                            )
                                        } else {
                                            originalListing?.copy(
                                                name = name,
                                                description = description,
                                                price = price.toDoubleOrNull() ?: 0.0,
                                                location = Location(address = address),
                                                amenities = amenities.toList(),
                                                localAttractions = attractions,
                                                coverPhoto = coverPhotoUrl,
                                                images = galleryImages
                                            ) ?: Listing(
                                                id = listingId,
                                                name = name,
                                                description = description,
                                                price = price.toDoubleOrNull() ?: 0.0,
                                                location = Location(address = address),
                                                amenities = amenities.toList(),
                                                localAttractions = attractions,
                                                coverPhoto = coverPhotoUrl,
                                                images = galleryImages
                                            )
                                        }

                                        val result = if (listingId == null) {
                                            FirebaseRepository.createListing(listing)
                                        } else {
                                            FirebaseRepository.updateListing(listing)
                                        }
                                        
                                        if (result.isSuccess) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Listing saved successfully!")
                                            }
                                            navController.popBackStack()
                                        } else {
                                            isSaving = false
                                            snackbarHostState.showSnackbar("Error: ${result.exceptionOrNull()?.message}")
                                        }
                                    } catch (e: Exception) {
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = KarGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(if (currentStep == steps.size - 1) "Complete Listing" else "Next Step")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stepper
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                steps.forEachIndexed { index, stepName ->
                    val isPast = index <= currentStep
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(if (isPast) KarGreen else Color.White, CircleShape)
                                .border(1.dp, if (isPast) KarGreen else Color.LightGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < currentStep) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("${index + 1}", color = if (isPast) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stepName, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (isPast) KarGreenDark else Color.Gray, maxLines = 1, softWrap = false)
                    }
                    if (index < steps.size - 1) {
                        Box(modifier = Modifier.weight(1f).height(2.dp).background(if (index < currentStep) KarGreen else Color.LightGray))
                    }
                }
            }

            Text(
                text = steps[currentStep],
                style = Typography.titleLarge,
                fontSize = 28.sp,
                color = KarGreenDark
            )

            AnimatedContent(targetState = currentStep) { step ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (step) {
                        0 -> {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Homestay Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = KarGreen)
                            )
                            Column {
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text("Location Address") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = KarOrange) },
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = KarGreen)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.clickable {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = KarGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Detect my location", color = KarGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("Starting Price (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = KarGreen)
                            )
                        }
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("COVER PHOTO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                                    Card(
                                        modifier = Modifier.fillMaxWidth().height(200.dp).clickable { coverPicker.launch("image/*") },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        if (localCoverUri != null || coverPhotoUrl.isNotEmpty()) {
                                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                                                coil.compose.AsyncImage(
                                                    model = localCoverUri ?: coverPhotoUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                                Surface(
                                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                                    color = Color.Black.copy(alpha = 0.5f),
                                                    shape = CircleShape
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp).size(16.dp))
                                                }
                                                if (coverPhotoUrl.isEmpty() && localCoverUri != null) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Upload Cover", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("PROPERTY GALLERY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                                    
                                    val galleryPicker = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.GetMultipleContents()
                                    ) { uris: List<Uri> ->
                                        if (uris.isNotEmpty()) {
                                            localGalleryUris = localGalleryUris + uris
                                            uris.forEach { uri ->
                                                scope.launch {
                                                    val path = "listings/gallery/${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
                                                    FirebaseRepository.uploadImage(context, uri, path).onSuccess { url ->
                                                        galleryImages = galleryImages + ListingImage(url = url)
                                                        localGalleryUris = localGalleryUris.filter { it != uri }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    ) {
                                        galleryImages.forEach { img ->
                                            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                                                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                                                    coil.compose.AsyncImage(
                                                        model = img.url,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                    Surface(
                                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).clickable {
                                                            galleryImages = galleryImages.filter { it.url != img.url }
                                                        },
                                                        color = Color.Black.copy(alpha = 0.5f),
                                                        shape = CircleShape
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp).size(12.dp))
                                                    }
                                                }
                                            }
                                        }

                                        localGalleryUris.forEach { uri ->
                                            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                                                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                                                    coil.compose.AsyncImage(
                                                        model = uri,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                        alpha = 0.5f
                                                    )
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.align(Alignment.Center).size(24.dp),
                                                        color = KarGreen
                                                    )
                                                }
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.size(100.dp).clickable { galleryPicker.launch("image/*") },
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                                            }
                                        }
                                    }
                                }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = KarOrangeLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, KarOrange.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = KarOrange, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Clear photos of your property increase inquiries by 40%.", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = KarOrange)
                                    }
                                }
                            }
                        }
                        2 -> {
                            val availableAmenities = listOf(
                                "WiFi", "Home Food", "Parking", "Power Backup", 
                                "Attach Bathroom", "Hot Water", "Trekking", "Campfire",
                                "Laundry", "Kitchen Access", "CCTV", "Garden", 
                                "Workstation", "EV Charging", "Pet Friendly", "Guide Service",
                                "Traditional Meals", "Yoga Space", "Bicycle Rental", "Indoor Games"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableAmenities.chunked(2).forEach { rowItems ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowItems.forEach { amenity ->
                                            val isSelected = amenities.contains(amenity)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) KarGreen.copy(alpha = 0.1f) else Color.White, RoundedCornerShape(12.dp))
                                                    .border(1.dp, if (isSelected) KarGreen else Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        amenities = if (isSelected) amenities - amenity else amenities + amenity
                                                    }
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(amenity, color = if (isSelected) KarGreenDark else Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Description", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    TextButton(
                                        onClick = {
                                            if (name.isNotBlank() && address.isNotBlank()) {
                                                scope.launch {
                                                    isGeneratingDescription = true
                                                    val result = GeminiService.generateDescription(
                                                        homestayName = name,
                                                        location = address,
                                                        amenities = amenities.toList(),
                                                        nearbyAttractions = emptyList()
                                                    )
                                                    result.onSuccess { generatedDesc ->
                                                        description = generatedDesc
                                                    }.onFailure { e ->
                                                        android.util.Log.e("AddEditListing", "AI generateDescription failed", e)
                                                    }
                                                    isGeneratingDescription = false
                                                }
                                            }
                                        },
                                        enabled = !isGeneratingDescription && name.isNotBlank() && address.isNotBlank()
                                    ) {
                                        if (isGeneratingDescription) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = KarOrange, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KarOrange, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isGeneratingDescription) "Generating..." else "AI Generate", color = KarOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (name.isBlank() || address.isBlank()) {
                                    Text("Fill in Name and Address first to use AI generation.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                                }
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    placeholder = { Text("Share what makes your stay special...") },
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE5E7EB), focusedBorderColor = KarGreen)
                                )
                            }
                        }
                        4 -> {
                            val safetyItems = listOf(
                                "Clean Drinking Water", "Fresh Bedding & Towels", "Safe Electric Wiring", 
                                "First Aid Kit", "Emergency Contacts", "Fire Extinguisher", 
                                "Smoke Detector", "Secure Locks", "External Lighting", 
                                "Local Hospital Info", "Mosquito Nets", "Safe Deposit Box"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                safetyItems.forEach { item ->
                                    val isSelectedItem = amenities.contains(item)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelectedItem) KarGreen.copy(alpha = 0.05f) else Color.White, RoundedCornerShape(12.dp))
                                            .border(1.dp, if (isSelectedItem) KarGreen.copy(alpha = 0.5f) else Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                            .clickable { 
                                                amenities = if (isSelectedItem) amenities - item else amenities + item
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item, fontSize = 14.sp)
                                        Box(
                                            modifier = Modifier.size(24.dp).background(if (isSelectedItem) KarGreen else Color.White, CircleShape).border(1.dp, if (isSelectedItem) KarGreen else Color.LightGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelectedItem) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                        5 -> {
                            // Secret spots step
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = KarOrangeLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, KarOrange.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KarOrange, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Add hidden gems or tourist spots near your homestay.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KarOrange)
                                    }
                                }

                                var newSpotName by remember { mutableStateOf("") }
                                var newSpotDist by remember { mutableStateOf("") }
                                var newSpotDesc by remember { mutableStateOf("") }

                                attractions.forEachIndexed { index, spot ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(spot.name, fontWeight = FontWeight.Bold, color = KarGreenDark)
                                                IconButton(
                                                    onClick = {
                                                        attractions = attractions.filterIndexed { i, _ -> i != index }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                                                }
                                            }
                                            Text("${spot.distance} km away", fontSize = 12.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(spot.description, fontSize = 14.sp, color = Color.DarkGray)
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = KarGreenLight.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, KarGreen.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            OutlinedTextField(
                                                value = newSpotName,
                                                onValueChange = { newSpotName = it },
                                                label = { Text("Spot Name", fontSize = 10.sp) },
                                                modifier = Modifier.weight(2f),
                                                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = Color.Transparent, focusedBorderColor = KarGreen)
                                            )
                                            OutlinedTextField(
                                                value = newSpotDist,
                                                onValueChange = { newSpotDist = it },
                                                label = { Text("Distance (Km)", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = Color.Transparent, focusedBorderColor = KarGreen)
                                            )
                                        }
                                        OutlinedTextField(
                                            value = newSpotDesc,
                                            onValueChange = { newSpotDesc = it },
                                            label = { Text("Description / Landmark", fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = Color.Transparent, focusedBorderColor = KarGreen)
                                        )
                                        Button(
                                            onClick = {
                                                if (newSpotName.isNotBlank() && newSpotDist.isNotBlank()) {
                                                    attractions = attractions + Attraction(
                                                        name = newSpotName,
                                                        distance = newSpotDist.toDoubleOrNull() ?: 0.0,
                                                        description = newSpotDesc
                                                    )
                                                    newSpotName = ""
                                                    newSpotDist = ""
                                                    newSpotDesc = ""
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.buttonColors(containerColor = KarGreen)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add Secret Spot")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
