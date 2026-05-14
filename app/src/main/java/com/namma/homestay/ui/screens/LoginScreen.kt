package com.namma.homestay.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.namma.homestay.models.UserProfile
import com.namma.homestay.repository.FirebaseRepository
import com.namma.homestay.ui.theme.*
import android.util.Log
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, initialRole: String) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<String?>(initialRole) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    val authenticate: () -> Unit = {
        if (email.isBlank() || password.isBlank() || (!isLoginMode && name.isBlank())) {
            errorMsg = "Please fill all fields"
        } else if (!isLoginMode && selectedRole == null) {
            errorMsg = "Please select a role"
        } else {
            isLoading = true
            errorMsg = null
            scope.launch {
                try {
                    val result = if (isLoginMode) {
                        FirebaseRepository.signInWithEmailPassword(email.trim(), password.trim())
                    } else {
                        FirebaseRepository.registerWithEmailPassword(email.trim(), password.trim(), name.trim())
                    }

                    if (result.isSuccess) {
                        val user = FirebaseRepository.getCurrentUser() ?: throw Exception("Auth succeeded but user not found")
                        
                        // Ensure role and sync metadata
                        val profile = FirebaseRepository.getUserProfile(user.uid)
                        if (profile == null) {
                            val newProfile = UserProfile(
                                uid = user.uid,
                                name = if (isLoginMode) (user.displayName ?: email.split("@")[0]) else name.trim(),
                                email = email.trim(),
                                role = initialRole,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            FirebaseRepository.updateUserProfile(newProfile)
                        } else if (profile.role != initialRole) {
                            FirebaseRepository.switchRole(initialRole)
                        } else {
                            // Update last login / updateAt
                            FirebaseRepository.updateUserProfile(profile)
                        }

                        val route = if (initialRole == "host") "dashboard" else "travelerHome"
                        navController.navigate(route) { 
                            popUpTo("welcome") { inclusive = true }
                        }
                    } else {
                        errorMsg = result.exceptionOrNull()?.message ?: "Authentication failed"
                    }
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Authentication error", e)
                    errorMsg = e.message ?: "Authentication failed"
                } finally {
                    isLoading = false
                }
            }
        }
    }


    val isHost = initialRole == "host"
    val themeColor = if (isHost) KarOrange else KarGreen
    val headerIcon = if (isHost) Icons.Default.Shield else Icons.Default.Map

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(themeColor, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(headerIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isHost) "Host Portal" else "Traveler Portal",
            style = Typography.titleLarge,
            fontSize = 28.sp,
            color = if (isHost) KarOrange else KarGreenDark
        )
        Text(
            if (isHost) "Manage your homestay and welcome guests." else "Discover authentic stays across Karnataka.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Button(
                onClick = { isLoginMode = true },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoginMode) themeColor else Color.Transparent,
                    contentColor = if (isLoginMode) Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(if (isLoginMode) 2.dp else 0.dp)
            ) {
                Text("LOGIN")
            }
            Button(
                onClick = { isLoginMode = false },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isLoginMode) themeColor else Color.Transparent,
                    contentColor = if (!isLoginMode) Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(if (!isLoginMode) 2.dp else 0.dp)
            ) {
                Text("REGISTER")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                AnimatedVisibility(visible = !isLoginMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Create your ${initialRole} account",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (errorMsg != null) {
                    Text(errorMsg!!, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = authenticate,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            if (isLoginMode) "Login as ${initialRole.replaceFirstChar { it.uppercase() }}" 
                            else "Register as ${initialRole.replaceFirstChar { it.uppercase() }}",
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Switch to ${if (isHost) "Traveler" else "Host"} Selection", color = Color.Gray)
        }
    }
}
