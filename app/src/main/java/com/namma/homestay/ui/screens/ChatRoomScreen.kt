package com.namma.homestay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.namma.homestay.models.*
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(navController: NavController, inquiryId: String) {
    val auth = Firebase.auth
    var inquiry by remember { mutableStateOf<Inquiry?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isHost by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(inquiryId) {
        if (inquiryId.isEmpty()) return@LaunchedEffect
        val currentUser = FirebaseRepository.getCurrentUser() ?: return@LaunchedEffect

        // Listen for inquiry updates
        scope.launch {
            try {
                FirebaseRepository.listenToInquiry(inquiryId).collect { data ->
                    if (data != null) {
                        inquiry = data
                        isHost = data.hostId == currentUser.uid
                        
                        // Mark as read if user is the receiver
                        if (isHost && !data.read) {
                            FirebaseRepository.markInquiryAsRead(inquiryId)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

        // Listen for messages
        scope.launch {
            try {
                FirebaseRepository.listenToMessages(inquiryId).collect { data ->
                    messages = data
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    if (inquiry == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KarGreen)
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    inquiry?.let { inq ->
                        Column {
                            Text(if (isHost) inq.travelerName else inq.listingName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${inq.status.uppercase()} • ${inq.guests} Guests", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    inquiry?.let { inq ->
                        if (inq.contactNumber.isNotEmpty()) {
                            IconButton(onClick = { /* TODO: Call Intent */ }) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", tint = KarGreen)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type your message...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = KarGreen
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && auth.currentUser != null && !isSending) {
                                val msgText = inputText
                                inputText = ""
                                isSending = true
                                scope.launch {
                                    val chatMessage = ChatMessage(
                                        senderId = auth.currentUser!!.uid,
                                        senderRole = if (isHost) "host" else "traveler",
                                        text = msgText
                                    )
                                    val result = FirebaseRepository.sendMessage(inquiryId, chatMessage)
                                    if (result.isFailure) {
                                        // Restore the text so the user can retry
                                        inputText = msgText
                                        snackbarHostState.showSnackbar(
                                            "Failed to send: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                                        )
                                    } else if (isHost && inquiry?.status == "sent") {
                                        FirebaseRepository.updateInquiryStatus(inquiryId, "responded")
                                    }
                                    isSending = false
                                }
                            }
                        },
                        modifier = Modifier.background(
                            if (isSending) KarGreen.copy(alpha = 0.5f) else KarGreen,
                            RoundedCornerShape(16.dp)
                        ).size(48.dp),
                        enabled = !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))
        ) {
            val currentInquiry = inquiry
            if (currentInquiry != null) {
                // Context Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dates: ${currentInquiry.checkIn} to ${currentInquiry.checkOut}", fontWeight = FontWeight.Bold, color = KarGreenDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total Estimate: ₹${currentInquiry.totalPrice}", color = KarOrange)
                        
                        if (isHost && currentInquiry.status == "sent") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        // Accept Logic
                                        val aiMessageText = "Namaste ${currentInquiry.travelerName}! I am happy to accept your reservation request. We look forward to hosting you!"
                                        scope.launch {
                                            FirebaseRepository.updateInquiryStatus(inquiryId, "accepted")
                                            FirebaseRepository.sendMessage(
                                                inquiryId,
                                                ChatMessage(
                                                    senderId = auth.currentUser!!.uid,
                                                    senderRole = "host",
                                                    text = aiMessageText
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Text("Accept")
                                }
                                Button(
                                    onClick = {
                                        val aiMessageText = "Thank you for reaching out, ${currentInquiry.travelerName}. Unfortunately, we are not available for the requested period."
                                        scope.launch {
                                            FirebaseRepository.updateInquiryStatus(inquiryId, "declined")
                                            FirebaseRepository.sendMessage(
                                                inquiryId,
                                                ChatMessage(
                                                    senderId = auth.currentUser!!.uid,
                                                    senderRole = "host",
                                                    text = aiMessageText
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                                ) {
                                    Text("Decline")
                                }
                            }
                        }
                        else if (currentInquiry.status == "accepted" || currentInquiry.status == "declined") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (currentInquiry.status == "accepted") "Booking Accepted" else "Request Declined",
                                color = if (currentInquiry.status == "accepted") Color(0xFF16A34A) else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.background(
                                    if (currentInquiry.status == "accepted") Color(0xFF16A34A).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                ).padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    val isOwn = msg.senderId == auth.currentUser?.uid
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .background(
                                    if (isOwn) KarGreen else Color.White,
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isOwn) 16.dp else 0.dp,
                                        bottomEnd = if (isOwn) 0.dp else 16.dp
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (isOwn) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
