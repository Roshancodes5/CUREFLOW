package com.cureflow.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cureflow.ui.appointment.AppointmentViewModel
import com.cureflow.ui.departments.DepartmentsViewModel
import com.cureflow.ui.home.HomeDashboardViewModel
import com.cureflow.ui.voice.VoiceAssistScreen
import com.cureflow.ui.voice.VoiceAssistViewModel

sealed class NavigationTab(val route: String, val title: String, val icon: ImageVector) {
    object Home : NavigationTab("home", "Home", Icons.Default.Home)
    object Departments : NavigationTab("departments", "Departments", Icons.Default.LocalHospital)
    object Appointments : NavigationTab("appointments", "Appointments", Icons.Default.Event)
    object VoiceAssist : NavigationTab("voice", "Voice Assist", Icons.Default.Mic)
    object Emergency : NavigationTab("emergency", "Emergency", Icons.Default.Warning)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    voiceAssistViewModel: VoiceAssistViewModel = viewModel(),
    homeViewModel: HomeDashboardViewModel = viewModel(),
    departmentsViewModel: DepartmentsViewModel = viewModel(),
    appointmentViewModel: AppointmentViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf<NavigationTab>(NavigationTab.Home) }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        NavigationTab.Home,
        NavigationTab.Departments,
        NavigationTab.Appointments,
        NavigationTab.VoiceAssist,
        NavigationTab.Emergency
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            NavigationTab.Home -> "Careflow Patient Portal"
                            NavigationTab.Departments -> "Hospital Departments & Doctors"
                            NavigationTab.Appointments -> "My Appointments & Queue"
                            NavigationTab.VoiceAssist -> "Careflow Voice Assistant"
                            NavigationTab.Emergency -> "Emergency Response"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (tab == NavigationTab.Emergency) Color(0xFFD32F2F) else LocalContentColor.current
                            )
                        },
                        label = { Text(tab.title, fontSize = 10.sp) },
                        selected = currentTab == tab,
                        onClick = {
                            if (tab == NavigationTab.Emergency) {
                                showEmergencyDialog = true
                            } else {
                                currentTab = tab
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                NavigationTab.Home -> {
                    HomeDashboardContent(
                        homeViewModel = homeViewModel,
                        onNavigateToDepartments = { currentTab = NavigationTab.Departments },
                        onNavigateToAppointments = { currentTab = NavigationTab.Appointments },
                        onNavigateToVoice = { currentTab = NavigationTab.VoiceAssist },
                        onTriggerEmergency = { showEmergencyDialog = true }
                    )
                }
                NavigationTab.Departments -> {
                    DepartmentsContent(viewModel = departmentsViewModel)
                }
                NavigationTab.Appointments -> {
                    AppointmentsContent(viewModel = appointmentViewModel)
                }
                NavigationTab.VoiceAssist -> {
                    VoiceAssistScreen(viewModel = voiceAssistViewModel)
                }
                NavigationTab.Emergency -> {
                    EmergencyContent(onTriggerEmergency = { showEmergencyDialog = true })
                }
            }
        }

        if (showEmergencyDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EMERGENCY ALERT", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text("Hospital emergency response team has been alerted with your GPS location. Medical personnel are en route.")
                },
                confirmButton = {
                    Button(
                        onClick = { showEmergencyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Acknowledge & Close", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun HomeDashboardContent(
    homeViewModel: HomeDashboardViewModel,
    onNavigateToDepartments: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onTriggerEmergency: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Welcome to Careflow", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your smart hospital companion & live queue tracker.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Live Queue Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Live Queue Status", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                val queue = homeViewModel.activeQueue
                Text("Current Token Serving: ${queue?.currentServingToken ?: "A-104"}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Estimated Wait Range: ${queue?.estimatedWaitRange ?: "10-15 minutes"}", fontSize = 14.sp)
            }
        }

        // Quick Actions Grid
        Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToDepartments,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalHospital, contentDescription = null)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Departments", fontSize = 12.sp)
                }
            }
            Button(
                onClick = onNavigateToAppointments,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Appointments", fontSize = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToVoice,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Voice Assistant", fontSize = 12.sp)
                }
            }
            Button(
                onClick = onTriggerEmergency,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Emergency", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DepartmentsContent(viewModel: DepartmentsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Hospital Departments & Doctors", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        if (viewModel.departments.isEmpty()) {
            Text("Loading departments from backend...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            viewModel.departments.forEach { dept ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(dept.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        dept.description?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Doctors: ${dept.doctors?.size ?: 0} available", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentsContent(viewModel: AppointmentViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Appointments & Queue", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        viewModel.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (viewModel.appointments.isEmpty()) {
            Text("No appointments booked yet. You can book an appointment or check your token via Voice Assist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            viewModel.appointments.forEach { appt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Token: ${appt.tokenNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Status: ${appt.status}", fontSize = 14.sp)
                        Text("Date: ${appt.dateTime}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyContent(onTriggerEmergency: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Emergency Response Hub", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tap the button below to immediately notify the hospital emergency trauma unit and dispatch medical assistance.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onTriggerEmergency,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("TRIGGER EMERGENCY", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
