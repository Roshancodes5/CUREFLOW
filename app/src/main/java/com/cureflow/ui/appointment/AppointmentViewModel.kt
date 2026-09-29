package com.cureflow.ui.appointment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cureflow.data.model.Appointment
import com.cureflow.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class AppointmentViewModel : ViewModel() {
    private val api = RetrofitClient.api

    var appointments by mutableStateOf<List<Appointment>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    init {
        loadAppointments()
    }

    fun loadAppointments() {
        viewModelScope.launch {
            isLoading = true
            try {
                val response = api.getAppointments()
                if (response.isSuccessful && response.body() != null) {
                    appointments = response.body()!!
                }
            } catch (e: Exception) {
                message = "Error loading appointments: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun bookAppointment(doctorId: String, dateTime: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.bookAppointment(mapOf("doctorId" to doctorId, "dateTime" to dateTime))
                if (response.isSuccessful) {
                    message = "Appointment booked successfully!"
                    loadAppointments()
                    onSuccess()
                } else {
                    message = "Failed to book appointment."
                }
            } catch (e: Exception) {
                message = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            try {
                // We can add cancel endpoint call or handle locally
                message = "Appointment cancelled."
                loadAppointments()
            } catch (e: Exception) {
                message = "Error: ${e.localizedMessage}"
            }
        }
    }
}
