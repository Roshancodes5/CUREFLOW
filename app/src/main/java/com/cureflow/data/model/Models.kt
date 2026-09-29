package com.cureflow.data.model

data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val phone: String?,
    val role: String
)

data class AuthResponse(
    val message: String,
    val token: String,
    val user: UserResponse
)

data class Department(
    val id: String,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val doctors: List<Doctor>?
)

data class Doctor(
    val id: String,
    val userId: String,
    val departmentId: String,
    val specialty: String,
    val roomNumber: String,
    val isAvailable: Boolean,
    val department: Department?,
    val user: DoctorUser?
)

data class DoctorUser(
    val name: String,
    val email: String,
    val phone: String?
)

data class Appointment(
    val id: String,
    val patientId: String,
    val doctorId: String,
    val dateTime: String,
    val status: String,
    val tokenNumber: String,
    val doctor: Doctor?
)

data class QueueStatus(
    val id: String,
    val doctorId: String,
    val currentServingToken: String,
    val patientsAheadCount: Int,
    val estimatedWaitRange: String,
    val delayReason: String?,
    val doctor: Doctor?
)

data class VoiceIntentResponse(
    val transcript: String,
    val actionType: String,
    val responseText: String,
    val payload: Any?
)
