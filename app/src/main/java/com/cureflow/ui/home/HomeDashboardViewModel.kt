package com.cureflow.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cureflow.data.model.Appointment
import com.cureflow.data.model.Department
import com.cureflow.data.model.QueueStatus
import com.cureflow.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class HomeDashboardViewModel : ViewModel() {
    private val api = RetrofitClient.api

    var departments by mutableStateOf<List<Department>>(emptyList())
        private set

    var appointments by mutableStateOf<List<Appointment>>(emptyList())
        private set

    var activeQueue by mutableStateOf<QueueStatus?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Fetch departments
                val deptResponse = api.getDepartments()
                if (deptResponse.isSuccessful && deptResponse.body() != null) {
                    departments = deptResponse.body()!!
                }

                // Fetch appointments
                val apptResponse = api.getAppointments()
                if (apptResponse.isSuccessful && apptResponse.body() != null) {
                    appointments = apptResponse.body()!!
                }

                // If there's an appointment, fetch its queue status, or fetch default doctor queue
                val firstDocId = departments.firstOrNull()?.doctors?.firstOrNull()?.id
                if (firstDocId != null) {
                    val queueResponse = api.getQueueStatus(firstDocId)
                    if (queueResponse.isSuccessful) {
                        activeQueue = queueResponse.body()
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.localizedMessage}. Using offline mode."
            } finally {
                isLoading = false
            }
        }
    }
}
