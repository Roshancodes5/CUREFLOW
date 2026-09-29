package com.cureflow.data.repository

import com.cureflow.data.model.*
import com.cureflow.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CareflowRepository {
    private val api = RetrofitClient.api

    suspend fun login(email: String, pass: String): AuthResponse? = withContext(Dispatchers.IO) {
        try {
            val response = api.login(mapOf("email" to email, "password" to pass))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                RetrofitClient.setAuthToken(body.token)
                body
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun processVoiceIntent(transcript: String, language: String): VoiceIntentResponse? = withContext(Dispatchers.IO) {
        try {
            val response = api.processVoiceIntent(mapOf("transcript" to transcript, "language" to language))
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getQueueStatus(doctorId: String): QueueStatus? = withContext(Dispatchers.IO) {
        try {
            val response = api.getQueueStatus(doctorId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
}
