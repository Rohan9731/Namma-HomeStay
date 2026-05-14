package com.namma.homestay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.namma.homestay.models.Inquiry
import com.namma.homestay.models.toTimestamp
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*

@Composable
fun InquiriesScreen(navController: NavController, isHost: Boolean) {
    val scope = rememberCoroutineScope()
    var inquiries by remember { mutableStateOf<List<Inquiry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var inquiryToDelete by remember { mutableStateOf<Inquiry?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    // Track locally deleted IDs so the realtime listener doesn't restore them
    // if Firestore's local cache reverts before the server confirms the delete.
    var locallyDeletedIds by remember { mutableStateOf(setOf<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isHost) {
        val currentUser = FirebaseRepository.getCurrentUser()
        if (currentUser != null) {
            FirebaseRepository.listenToInquiries(currentUser.uid, isHost).collect { data ->
                inquiries = data
                    .filter { it.id !in locallyDeletedIds }
                    .sortedByDescending { it.updatedAt.toTimestamp() }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "MESSAGES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = KarOrange,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = if (isHost) "Guest Leads" else "My Chats",
                        style = Typography.titleLarge,
                        fontSize = 32.sp,
                        color = KarGreenDark
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KarGreen)
                    }
                }
            } else if (inquiries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No active conversations.", color = Color.Gray, fontWeight = FontWeight.Medium)
                            if (!isHost) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { navController.navigate("travelerHome") },
                                    colors = ButtonDefaults.buttonColors(containerColor = KarGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Explore Homestays")
                                }
                            }
                        }
                    }
                }
            } else {
                items(inquiries) { inquiry ->
                    val displayName = if (isHost) inquiry.travelerName else inquiry.listingName
                    val displayImage = if (isHost) "https://ui-avatars.com/api/?name=${inquiry.travelerName}&background=random" else inquiry.listingImage.ifEmpty { "https://images.unsplash.com/photo-1598332896317-5181330cc5aa" }
                    val statusColor = when (inquiry.status) {
                        "accepted" -> Color(0xFF16A34A)
                        "declined" -> Color.Red
                        else -> KarOrange
                    }
                    val isUnread = !inquiry.read && isHost

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("chatRoom/${inquiry.id}") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isUnread) KarOrangeLight.copy(alpha = 0.5f) else Color.White),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isUnread) 2.dp else 1.dp,
                            color = if (isUnread) KarOrange else Color(0xFFE5E7EB)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = displayImage,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(displayName, style = Typography.titleLarge, fontSize = 20.sp, color = KarGreenDark)
                                    if (isUnread) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.size(8.dp).background(KarOrange, CircleShape))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(inquiry.status.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = statusColor, letterSpacing = 1.sp)
                                if (inquiry.message.isNotBlank() || inquiry.lastMessage.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = inquiry.lastMessage.ifBlank { inquiry.message },
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { 
                                inquiryToDelete = inquiry
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }

    if (inquiryToDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) inquiryToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this chat with ${if (isHost) inquiryToDelete?.travelerName else inquiryToDelete?.listingName}? This will permanently remove all messages.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = inquiryToDelete?.id ?: return@Button
                        isDeleting = true
                        // Immediately mark as deleted locally so the listener can't restore it
                        locallyDeletedIds = locallyDeletedIds + id
                        inquiries = inquiries.filter { it.id != id }
                        inquiryToDelete = null  // Dismiss dialog immediately for instant feedback
                        scope.launch {
                            val result = FirebaseRepository.deleteInquiry(id)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("Conversation deleted")
                            } else {
                                // Rollback: allow it to reappear if delete truly failed
                                locallyDeletedIds = locallyDeletedIds - id
                                snackbarHostState.showSnackbar("Failed to delete: ${result.exceptionOrNull()?.message}")
                            }
                            isDeleting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !isDeleting
                ) {
                    Text(if (isDeleting) "Deleting..." else "Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { inquiryToDelete = null },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}
