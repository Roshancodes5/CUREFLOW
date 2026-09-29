package com.cureflow.data.remote

import com.cureflow.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface CareflowApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: Map<String, String>): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getProfile(): Response<UserResponse>

    @GET("api/hospitals/departments")
    suspend fun getDepartments(): Response<List<Department>>

    @GET("api/hospitals/doctors")
    suspend fun getDoctors(@Query("departmentId") departmentId: String? = null): Response<List<Doctor>>

    @GET("api/appointments")
    suspend fun getAppointments(): Response<List<Appointment>>

    @POST("api/appointments")
    suspend fun bookAppointment(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("api/queue/doctor/{doctorId}")
    suspend fun getQueueStatus(@Path("doctorId") doctorId: String): Response<QueueStatus>

    @POST("api/voice/intent")
    suspend fun processVoiceIntent(@Body request: Map<String, String>): Response<VoiceIntentResponse>

    @POST("api/emergency/dispatch")
    suspend fun dispatchEmergency(@Body request: Map<String, Any>): Response<Map<String, Any>>
}
