package com.namma.homestay.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.namma.homestay.ai.FoodAnalysisResult
import com.namma.homestay.ai.GeminiService
import com.namma.homestay.models.Dish
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIFoodAnalysisScreen(navController: NavController, listingId: String) {
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var analysisResult by remember { mutableStateOf<FoodAnalysisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            analysisResult = null
            errorMessage = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri ->
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(imageUri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    if (bitmap != null) {
                        capturedImage = bitmap
                        analysisResult = null
                        errorMessage = null
                    } else {
                        errorMessage = "Could not load image from gallery."
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to load image: ${e.message}"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analyze Food Photo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                        Text("AI-POWERED DISH IDENTIFICATION", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Camera Capture Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (capturedImage == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .background(Color(0xFFF8F9FA), RoundedCornerShape(16.dp))
                                .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(80.dp).background(KarGreen.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = KarGreen, modifier = Modifier.size(36.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Capture a dish photo", color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("Gemini AI will identify the dish", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = capturedImage!!.asImageBitmap(),
                                contentDescription = "Captured dish",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    capturedImage = null
                                    analysisResult = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KarGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Camera", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KarGreen)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = KarGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gallery", color = KarGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Analyze Button
            AnimatedVisibility(visible = capturedImage != null && analysisResult == null && !isAnalyzing) {
                Button(
                    onClick = {
                        scope.launch {
                            isAnalyzing = true
                            errorMessage = null
                            try {
                                val stream = ByteArrayOutputStream()
                                capturedImage!!.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                                val imageBytes = stream.toByteArray()

                                val result = GeminiService.analyzeFoodPhoto(imageBytes)
                                result.onSuccess { foodResult ->
                                    analysisResult = foodResult
                                }.onFailure { error ->
                                    errorMessage = error.message ?: "Analysis failed"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "An error occurred"
                            }
                            isAnalyzing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KarOrange),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze with Gemini AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Loading State
            AnimatedVisibility(visible = isAnalyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KarOrangeLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = KarOrange, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Analyzing your dish...", fontWeight = FontWeight.Bold, color = KarOrange)
                            Text("Gemini AI is identifying the dish and creating a description.", fontSize = 12.sp, color = KarOrange)
                        }
                    }
                }
            }

            // Error Message
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(errorMessage ?: "", color = Color.Red, fontSize = 14.sp)
                    }
                }
            }

            // Analysis Result
            AnimatedVisibility(visible = analysisResult != null) {
                analysisResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KarGreen.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(KarGreen.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = KarGreen, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(result.dishName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                                    Text(result.category.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = KarOrange, letterSpacing = 1.sp)
                                }
                            }

                            Divider(color = Color(0xFFE5E7EB))

                            Text(result.description, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SUGGESTED PRICE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                    Text("₹${result.suggestedPrice.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KarGreen)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DIETARY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                    Row {
                                        if (result.isVeg) {
                                            Box(modifier = Modifier.background(KarGreen, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                Text("VEG", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (result.isSpicy) {
                                            Box(modifier = Modifier.background(KarOrange, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                Text("SPICY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    // Navigate back to menu update with the analyzed dish
                                    navController.previousBackStackEntry?.savedStateHandle?.set(
                                        "aiDish",
                                        Dish(
                                            name = result.dishName,
                                            description = result.description,
                                            category = result.category,
                                            price = result.suggestedPrice,
                                            isVeg = result.isVeg,
                                            isSpicy = result.isSpicy,
                                            veg = result.isVeg,
                                            spicy = result.isSpicy
                                        )
                                    )
                                    navController.popBackStack()
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KarGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add to Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
