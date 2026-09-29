package com.cureflow.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistScreen(
    viewModel: VoiceAssistViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    // Pulse animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (viewModel.currentLanguage.startsWith("ta")) "கேர்ஃப்ளோ குரல் உதவியாளர்" else "Careflow Voice Assistant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (viewModel.currentLanguage.startsWith("ta")) "மருத்துவமனை குரல் வழிகாட்டி" else "Hospital Voice Guide (Slides 36-38)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Language Switch Button
                    TextButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.semantics {
                            contentDescription = if (viewModel.currentLanguage.startsWith("ta")) "Switch to English" else "தமிிழுக்கு மாற்றுக"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (viewModel.currentLanguage.startsWith("ta")) "EN" else "தமிழ்",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            // Always Accessible Emergency Button at the bottom
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.triggerEmergency() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F) // Deep Emergency Red / Material Error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .semantics {
                                contentDescription = "Emergency button. Tap for immediate hospital emergency response."
                            },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (viewModel.currentLanguage.startsWith("ta")) "அவசரம் (EMERGENCY)" else "EMERGENCY / அவசரம்",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Status / Listening Indicator
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isListening)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                viewModel.isListening -> if (viewModel.currentLanguage.startsWith("ta")) "கேட்கிறது... பேசவும்" else "Listening... Speak now"
                                viewModel.isSpeaking -> if (viewModel.currentLanguage.startsWith("ta")) "கேர்ஃப்ளோ பேசுகிறது..." else "Careflow is speaking..."
                                else -> if (viewModel.currentLanguage.startsWith("ta")) "மைக்கைத் தட்டிப் பேசத் தொடங்கவும்" else "Tap microphone to speak"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (viewModel.isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (!viewModel.errorMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = viewModel.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Conversation Transcript Cards (User Spoken & Careflow Response)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // User Spoken Text Box
                    if (viewModel.spokenText.isNotBlank()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (viewModel.currentLanguage.startsWith("ta")) "நீங்கள் கூறியது:" else "You said:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${viewModel.spokenText}\"",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Careflow Response Box
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (viewModel.currentLanguage.startsWith("ta")) "கேர்ஃப்ளோ பதில்:" else "Careflow Response:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.responseText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Sample Commands Hint Chips
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (viewModel.currentLanguage.startsWith("ta")) "மாதிரி கட்டளைகள்: 'டோக்கன் எங்கே', 'எவ்வளவு நேரம்', 'மீண்டும்', 'நிறுத்து', 'அவசரம்'"
                        else "Sample Commands: \"Where is my token?\", \"How long is my wait?\", \"Repeat\", \"Stop\", \"Emergency\"",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Center Large Microphone Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    if (viewModel.isListening) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (hasAudioPermission) {
                                if (viewModel.isListening) {
                                    viewModel.stopListening()
                                } else {
                                    viewModel.startListening()
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(88.dp)
                            .semantics {
                                contentDescription = if (viewModel.isListening) "Stop listening microphone" else "Start listening microphone"
                            },
                        containerColor = if (viewModel.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (viewModel.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            // Emergency Active Dialog / Banner
            if (viewModel.isEmergencyActive) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissEmergency() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (viewModel.currentLanguage.startsWith("ta")) "அவசர எச்சரிக்கை!" else "EMERGENCY ALERT!",
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Text(
                            text = if (viewModel.currentLanguage.startsWith("ta"))
                                "மருத்துவமனை அவசரக் குழு மற்றும் உங்கள் முதன்மை பராமரிப்பாளர் எச்சரிக்கப்பட்டுள்ளனர். மருத்துவ ஊழியர்கள் உடனடியாக உங்களிடம் வருவார்கள்."
                            else
                                "Hospital emergency response team and your designated caregiver have been alerted. Medical staff are en route to your location.",
                            fontSize = 16.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissEmergency() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text(
                                text = if (viewModel.currentLanguage.startsWith("ta")) "மூடு / உறுதிப்படுத்து" else "Acknowledge / Close",
                                color = Color.White
                            )
                        }
                    }
                )
            }
        }
    }
}
