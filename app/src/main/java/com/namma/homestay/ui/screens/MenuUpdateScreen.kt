package com.namma.homestay.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.namma.homestay.ai.GeminiService
import com.namma.homestay.models.Dish
import com.namma.homestay.models.Menu
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuUpdateScreen(navController: NavController, listingId: String) {
    var activeTab by remember { mutableStateOf("Breakfast") }
    var dishes by remember { mutableStateOf<List<Dish>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isAdding by remember { mutableStateOf(false) }
    var isGeneratingDesc by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var isSavingMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Add Dish Form State
    var newDishName by remember { mutableStateOf("") }
    var newDishDesc by remember { mutableStateOf("") }
    var newDishPrice by remember { mutableStateOf("") }
    var newDishImageUrl by remember { mutableStateOf("") }
    var localDishImageUri by remember { mutableStateOf<Uri?>(null) }
    var isVeg by remember { mutableStateOf(true) }
    var isSpicy by remember { mutableStateOf(false) }

    val categories = listOf("Breakfast", "Lunch", "Snacks", "Dinner")

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())
    val menuDocId = "${listingId}_$todayStr"

    // Launcher for dish image upload in text form
    val dishImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            localDishImageUri = it
            scope.launch {
                val path = "dishes/${System.currentTimeMillis()}.jpg"
                FirebaseRepository.uploadImage(context, it, path).onSuccess { url ->
                    newDishImageUrl = url
                    localDishImageUri = null
                }
            }
        }
    }

    // Speech recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = results?.firstOrNull() ?: return@rememberLauncherForActivityResult
            scope.launch {
                isListening = false
                try {
                    val extractResult = GeminiService.extractDishesFromVoice(recognizedText, activeTab)
                    extractResult.onSuccess { dishList ->
                        val updatedDishes = dishes + dishList
                        dishes = updatedDishes
                        val menu = Menu(
                            id = menuDocId,
                            listingId = listingId,
                            hostId = FirebaseRepository.getCurrentUser()?.uid ?: "",
                            date = todayStr,
                            dishes = updatedDishes.map { it.copy(imageUrl = null) }
                        )
                        FirebaseRepository.saveMenu(menu)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MenuUpdate", "Voice processing failed", e)
                }
            }
        } else {
            isListening = false
        }
    }

    // Microphone permission launcher
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say the dish name and details...")
            }
            speechLauncher.launch(intent)
            isListening = true
        }
    }

    // Check for AI analyzed dish from navigation
    val aiDish = navController.currentBackStackEntry?.savedStateHandle?.get<Dish>("aiDish")
    LaunchedEffect(aiDish) {
        aiDish?.let { dish ->
            val updatedDishes = dishes + dish.copy(category = activeTab)
            dishes = updatedDishes
            val menu = Menu(
                id = menuDocId,
                listingId = listingId,
                hostId = FirebaseRepository.getCurrentUser()?.uid ?: "",
                date = todayStr,
                dishes = updatedDishes.map { it.copy(imageUrl = null) }
            )
            FirebaseRepository.saveMenu(menu)
            navController.currentBackStackEntry?.savedStateHandle?.remove<Dish>("aiDish")
        }
    }

    LaunchedEffect(listingId) {
        val menu = FirebaseRepository.getMenu(listingId, todayStr)
        if (menu != null) {
            dishes = menu.dishes
        }
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Today's Kitchen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                        Text("Update your guests on what's cooking!", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp).background(KarGreenLight, RoundedCornerShape(12.dp)).padding(8.dp)) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = KarGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(activeTab),
                containerColor = Color.White,
                edgePadding = 16.dp,
                indicator = {}
            ) {
                categories.forEachIndexed { index, category ->
                    val selected = activeTab == category
                    Tab(
                        selected = selected,
                        onClick = { activeTab = category },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selected) KarOrange else Color.Transparent,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) KarOrange else Color.LightGray,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.uppercase(),
                                color = if (selected) Color.White else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Input Method Selection
            if (!isAdding) {
                Row(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { navController.navigate("aiFoodAnalysis/$listingId") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say the dish name and details...")
                                }
                                speechLauncher.launch(intent)
                                isListening = true
                            } else {
                                voicePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isListening) "Listening..." else "Voice", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Text", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Text Entry Button / Form
            Box(modifier = Modifier.padding(16.dp)) {
                if (!isAdding) {
                    // Show a hint when not adding
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = KarGreenLight.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = KarGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Select a method above to add dishes to your menu. Use Photo for AI-powered dish identification!", fontSize = 12.sp, color = KarGreenDark)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = KarGreenLight),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Quick Text Entry", fontWeight = FontWeight.Bold, color = KarGreenDark)
                                IconButton(onClick = { isAdding = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = newDishName,
                                onValueChange = { newDishName = it },
                                label = { Text("Dish Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newDishPrice,
                                onValueChange = { newDishPrice = it },
                                label = { Text("Price (₹)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Description", fontSize = 12.sp, color = Color.Gray)
                                TextButton(
                                    onClick = {
                                        if (newDishName.isNotBlank()) {
                                            scope.launch {
                                                isGeneratingDesc = true
                                                val result = GeminiService.generateDishDescription(newDishName)
                                                result.onSuccess { desc ->
                                                    newDishDesc = desc
                                                }
                                                isGeneratingDesc = false
                                            }
                                        }
                                    },
                                    enabled = newDishName.isNotBlank() && !isGeneratingDesc
                                ) {
                                    if (isGeneratingDesc) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = KarOrange, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KarOrange, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isGeneratingDesc) "Generating..." else "AI Desc", color = KarOrange, fontSize = 10.sp)
                                }
                            }
                            OutlinedTextField(
                                value = newDishDesc,
                                onValueChange = { newDishDesc = it },
                                placeholder = { Text("Describe the dish...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isVeg, onCheckedChange = { isVeg = it }, colors = CheckboxDefaults.colors(checkedColor = KarGreen))
                                Text("Vegetarian", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Checkbox(checked = isSpicy, onCheckedChange = { isSpicy = it }, colors = CheckboxDefaults.colors(checkedColor = KarOrange))
                                Text("Spicy", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // Optional dish image upload
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (localDishImageUri != null || newDishImageUrl.isNotEmpty()) {
                                    Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) {
                                        AsyncImage(
                                            model = if (newDishImageUrl.isNotEmpty()) newDishImageUrl else localDishImageUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            alpha = if (localDishImageUri != null && newDishImageUrl.isEmpty()) 0.5f else 1f
                                        )
                                        if (localDishImageUri != null && newDishImageUrl.isEmpty()) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp).align(Alignment.Center),
                                                color = KarGreen,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                OutlinedButton(
                                    onClick = { dishImagePicker.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, KarGreen)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp), tint = KarGreen)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Photo (Optional)", fontSize = 11.sp, color = KarGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (newDishName.isNotBlank() && newDishPrice.isNotBlank() && FirebaseRepository.isLoggedIn()) {
                                        val newDish = Dish(
                                            name = newDishName,
                                            description = newDishDesc.ifBlank { "Fresh and delicious home-cooked meal." },
                                            category = activeTab,
                                            price = newDishPrice.toDoubleOrNull() ?: 0.0,
                                            imageUrl = newDishImageUrl.ifEmpty { null },
                                            isVeg = isVeg,
                                            isSpicy = isSpicy,
                                            veg = isVeg,
                                            spicy = isSpicy
                                        )
                                        val updatedDishes = dishes + newDish
                                        dishes = updatedDishes
                                        val menu = Menu(
                                            id = menuDocId,
                                            listingId = listingId,
                                            hostId = FirebaseRepository.getCurrentUser()?.uid ?: "",
                                            date = todayStr,
                                            dishes = updatedDishes.map { it.copy(imageUrl = null) }
                                        )
                                        scope.launch {
                                            isSavingMenu = true
                                            val result = FirebaseRepository.saveMenu(menu)
                                            if (result.isSuccess) {
                                                snackbarHostState.showSnackbar("Dish added to menu")
                                                newDishName = ""
                                                newDishDesc = ""
                                                newDishPrice = ""
                                                newDishImageUrl = ""
                                                localDishImageUri = null
                                                isAdding = false
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to update menu")
                                            }
                                            isSavingMenu = false
                                        }
                                    }
                                },
                                enabled = newDishName.isNotBlank() && newDishPrice.isNotBlank() && !isSavingMenu,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KarGreen)
                            ) {
                                if (isSavingMenu) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add To Menu", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Dishes List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KarGreen)
                }
            } else {
                val filteredDishes = dishes.filter { it.category == activeTab }
                if (filteredDishes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No dishes added for $activeTab", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        itemsIndexed(filteredDishes) { index, dish ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (dish.imageUrl != null) {
                                        AsyncImage(
                                            model = dish.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    } else {
                                        Box(modifier = Modifier.size(60.dp).background(KarGreenLight.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = KarGreen, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dish.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                                        Text(dish.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${dish.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KarOrange)
                                        IconButton(
                                            onClick = {
                                                // Delete dish — update local state immediately
                                                val updatedDishes = dishes.filterIndexed { i, _ -> i != dishes.indexOf(dish) }
                                                dishes = updatedDishes
                                                val menu = Menu(
                                                    id = menuDocId,
                                                    listingId = listingId,
                                                    hostId = FirebaseRepository.getCurrentUser()?.uid ?: "",
                                                    date = todayStr,
                                                    dishes = updatedDishes.map { it.copy(imageUrl = null) }
                                                )
                                                scope.launch {
                                                    FirebaseRepository.saveMenu(menu)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
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
}
