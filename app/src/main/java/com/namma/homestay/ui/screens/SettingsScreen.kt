package com.namma.homestay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.namma.homestay.ai.GeminiService
import com.namma.homestay.models.UserProfile
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }

    LaunchedEffect(Unit) {
        val uid = FirebaseRepository.getCurrentUser()?.uid
        if (uid != null) {
            userProfile = FirebaseRepository.getUserProfile(uid)
            selectedLanguage = userProfile?.languagePreference ?: "English"
        }
        isLoading = false
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = selectedLanguage,
            onLanguageSelected = { language ->
                selectedLanguage = language
                showLanguageDialog = false
                // Save to user preferences
                userProfile?.let { profile ->
                    scope.launch {
                        FirebaseRepository.updateUserProfile(profile.copy(languagePreference = language))
                    }
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACCOUNT & PREFERENCES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = KarOrange,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Settings",
                            style = Typography.titleLarge,
                            fontSize = 32.sp,
                            color = KarGreenDark
                        )
                    }
                    TextButton(
                        onClick = {
                            FirebaseRepository.signOut()
                            navController.navigate("welcome") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LOGOUT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(64.dp).background(KarGreen.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userProfile?.profilePhoto?.isNotEmpty() == true) {
                                    // Would use AsyncImage here
                                    Icon(Icons.Default.Person, contentDescription = null, tint = KarGreen, modifier = Modifier.size(32.dp))
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = KarGreen, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    userProfile?.name ?: (FirebaseRepository.getCurrentUser()?.displayName ?: "User"),
                                    style = Typography.titleLarge,
                                    fontSize = 20.sp,
                                    color = KarGreenDark
                                )
                                Text(
                                    (userProfile?.role ?: "TRAVELER").uppercase(),
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(onClick = { /* Edit profile */ }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFFE5E7EB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("EMAIL", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(
                                            userProfile?.email ?: (FirebaseRepository.getCurrentUser()?.email ?: "Not set"),
                                            fontSize = 14.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("PHONE", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(
                                            userProfile?.contactNumber?.ifEmpty { "Not set" } ?: "Not set",
                                            fontSize = 14.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // App Preferences
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("APP PREFERENCES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 2.sp)

                    // Language
                    PreferenceItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = selectedLanguage.uppercase(),
                        onClick = { showLanguageDialog = true }
                    )

                    // Notifications
                    PreferenceItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "ENABLED",
                        onClick = { }
                    )

                    // Privacy & Security
                    PreferenceItem(
                        icon = Icons.Default.Security,
                        title = "Privacy & Security",
                        subtitle = "VERIFIED",
                        onClick = { }
                    )

                    // Payment Methods
                    PreferenceItem(
                        icon = Icons.Default.CreditCard,
                        title = "Payment Methods",
                        subtitle = "ADD CARD",
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
fun PreferenceItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFF8F9FA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = KarGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, color = Color.DarkGray, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        Triple("English", "ENGLISH", "en"),
        Triple("Kannada", "ಕನ್ನಡ", "kn"),
        Triple("Hindi", "हिन्दी", "hi")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Language", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarGreenDark)
                Text("GEMINI POWERED TRANSLATION", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { (name, native, code) ->
                    val isSelected = currentLanguage == name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(name) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) KarGreen else Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(
                                    if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFF8F9FA),
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null,
                                    tint = if (isSelected) Color.White else KarGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else KarGreenDark)
                                Text(native, fontSize = 12.sp, color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
