package com.namma.homestay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.namma.homestay.models.Listing
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun BlockDatesScreen(navController: NavController, listingId: String) {
    val scope = rememberCoroutineScope()
    var listing by remember { mutableStateOf<Listing?>(null) }
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var blockedDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(listingId) {
        val fetchedListing = FirebaseRepository.getListing(listingId)
        if (fetchedListing != null) {
            listing = fetchedListing
            if (fetchedListing.rooms.isNotEmpty()) {
                selectedRoomId = fetchedListing.rooms.first().id
                blockedDates = fetchedListing.rooms.first().blockedDates
            }
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(Color.White, CircleShape)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "MARK DATES AS BOOKED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Gray,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Block Availability",
                            style = Typography.titleLarge,
                            fontSize = 24.sp,
                            color = KarGreenDark
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KarGreen)
                    }
                }
            } else {
                item {
                    Text("SELECT ROOM", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    listing?.rooms?.forEach { room ->
                        val isSelected = selectedRoomId == room.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable {
                                    selectedRoomId = room.id
                                    blockedDates = room.blockedDates
                                },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Color.White else Color(0xFFF8F9FA)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) KarOrange else Color(0xFFE5E7EB))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(if (isSelected) KarOrange else Color.White, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Bed, contentDescription = null, tint = if (isSelected) Color.White else Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(room.name, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    Text("₹${room.price} • ${room.capacity} Guests", fontSize = 10.sp, color = Color.Gray)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KarOrange)
                                }
                            }
                        }
                    }
                }

                item {
                    if (selectedRoomId != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${currentDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${currentDate.get(Calendar.YEAR)}",
                                        style = Typography.titleLarge,
                                        fontSize = 18.sp
                                    )
                                    Row {
                                        IconButton(onClick = {
                                            val newDate = currentDate.clone() as Calendar
                                            newDate.add(Calendar.MONTH, -1)
                                            currentDate = newDate
                                        }) { Icon(Icons.Default.ChevronLeft, contentDescription = null) }
                                        IconButton(onClick = {
                                            val newDate = currentDate.clone() as Calendar
                                            newDate.add(Calendar.MONTH, 1)
                                            currentDate = newDate
                                        }) { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                val calendar = currentDate.clone() as Calendar
                                calendar.set(Calendar.DAY_OF_MONTH, 1)
                                val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
                                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                                val days = mutableListOf<String>()
                                for (i in 0 until firstDayOfWeek) days.add("")
                                for (i in 1..daysInMonth) days.add(i.toString())

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    days.chunked(7).forEach { week ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            for (i in 0..6) {
                                                val dayStr = week.getOrNull(i) ?: ""
                                                Box(
                                                    modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(if (dayStr.isNotEmpty()) Color(0xFFF8F9FA) else Color.Transparent).clickable(enabled = dayStr.isNotEmpty()) {
                                                        val f = String.format("%04d-%02d-%02d", currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH) + 1, dayStr.toInt())
                                                        blockedDates = if (blockedDates.contains(f)) blockedDates - f else blockedDates + f
                                                    },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (dayStr.isNotEmpty()) {
                                                        val f = String.format("%04d-%02d-%02d", currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH) + 1, dayStr.toInt())
                                                        val isBlocked = blockedDates.contains(f)
                                                        Text(dayStr, fontSize = 12.sp, color = if (isBlocked) Color.Gray else Color.DarkGray, fontWeight = if (isBlocked) FontWeight.Normal else FontWeight.Bold)
                                                        if (isBlocked) {
                                                            Box(modifier = Modifier.fillMaxWidth(0.6f).height(2.dp).background(Color.Red))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Divider(color = Color(0xFFE5E7EB))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("DATES SELECTED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                        Text("${blockedDates.size} Days", style = Typography.titleLarge, fontSize = 20.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val updatedRooms = listing!!.rooms.map { room ->
                                                if (room.id == selectedRoomId) {
                                                    room.copy(
                                                        blockedDates = blockedDates,
                                                        status = if (blockedDates.isNotEmpty()) "occupied" else "available"
                                                    )
                                                } else room
                                            }
                                            scope.launch {
                                                val anyOccupied = updatedRooms.any { it.status == "occupied" }
                                                val allAvailable = updatedRooms.all { it.status == "available" }
                                                val occupancyStatus = when {
                                                    allAvailable -> "available"
                                                    anyOccupied -> "occupied"
                                                    else -> "partial"
                                                }

                                                val updatedListing = listing!!.copy(
                                                    rooms = updatedRooms,
                                                    occupancyStatus = occupancyStatus
                                                )
                                                FirebaseRepository.updateListing(updatedListing)
                                                navController.popBackStack()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = KarGreen)
                                    ) {
                                        Text("Save")
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
