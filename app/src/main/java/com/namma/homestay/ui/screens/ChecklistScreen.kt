package com.namma.homestay.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.namma.homestay.ui.theme.*

data class ChecklistCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val items: List<String>)

val categories = listOf(
    ChecklistCategory(
        "Room Preparation",
        Icons.Default.Home,
        listOf("Fresh linens and towels", "Drinking water in room", "Waste bin emptied", "Light & Fan check")
    ),
    ChecklistCategory(
        "Kitchen & Dining",
        Icons.Default.RestaurantMenu,
        listOf("Today's ingredients ready", "Dining area cleaned", "Menu updated in app")
    ),
    ChecklistCategory(
        "Safety & Comfort",
        Icons.Default.Shield,
        listOf("First aid kit accessible", "Emergency contacts displayed", "Gate locked if necessary", "Evening lighting on")
    ),
    ChecklistCategory(
        "Final Touches",
        Icons.Default.Favorite,
        listOf("Welcome drink ready", "Room fragrant & fresh", "Keys ready for handover")
    )
)

@Composable
fun ChecklistScreen(navController: NavController) {
    var checkedItems by remember { mutableStateOf(setOf<String>()) }

    val totalItems = categories.sumOf { it.items.size }
    val progress = if (totalItems > 0) (checkedItems.size.toFloat() / totalItems) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress)

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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(Color.White, CircleShape)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "PRE-ARRIVAL VERIFICATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = KarOrange,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Stay Ready Checklist",
                                style = Typography.titleLarge,
                                fontSize = 28.sp,
                                color = KarGreenDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ensure the best experience for your upcoming guests.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text("${(animatedProgress * 100).toInt()}%", style = Typography.titleLarge, fontSize = 32.sp, color = KarGreenDark)
                                Text("TOTAL PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${checkedItems.size} / $totalItems", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KarGreen)
                                Text("ITEMS COMPLETED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                            }
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Color(0xFFF9F7F2)).border(1.dp, Color(0xFFE8E4D8), CircleShape)) {
                            Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(KarGreen, CircleShape))
                        }
                    }
                }
            }

            categories.forEach { category ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(category.icon, contentDescription = null, tint = KarOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(category.title, style = Typography.titleLarge, fontSize = 20.sp, color = KarGreenDark)
                        }
                        
                        category.items.forEach { item ->
                            val isChecked = checkedItems.contains(item)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checkedItems = if (isChecked) checkedItems - item else checkedItems + item
                                    },
                                colors = CardDefaults.cardColors(containerColor = if (isChecked) KarGreenLight else Color.White),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, if (isChecked) KarGreen.copy(alpha = 0.2f) else Color(0xFFE5E7EB))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isChecked) KarGreenDark else Color.DarkGray,
                                        textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(if (isChecked) KarGreen else Color(0xFFF9F7F2), RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isChecked) Color.Transparent else Color(0xFFE8E4D8), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isChecked) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (checkedItems.size == totalItems) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = KarGreen),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.size(80.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                            }
                            Text("You're all set!", style = Typography.titleLarge, fontSize = 24.sp, color = Color.White)
                            Text("Everything is ready for your guests to have a wonderful stay.", color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate("dashboard") { popUpTo(0) } },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = KarGreen)
                            ) {
                                Text("Back to Dashboard", fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
