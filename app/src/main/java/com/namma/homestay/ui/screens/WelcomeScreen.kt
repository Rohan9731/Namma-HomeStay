package com.namma.homestay.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.namma.homestay.ui.theme.*

@Composable
fun WelcomeScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image (Using placeholder color for now as we don't have local assets)
        Box(modifier = Modifier.fillMaxSize().background(KarGreenDark))
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 300f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Namma",
                style = Typography.titleLarge,
                fontSize = 48.sp,
                color = KarOrange
            )
            Text(
                text = "HomeStay",
                style = Typography.titleLarge,
                fontSize = 48.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Experience the warmth of local hospitality in the heart of Karnataka.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = { navController.navigate("roleSelection") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KarGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
