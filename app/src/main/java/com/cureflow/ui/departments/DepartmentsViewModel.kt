package com.cureflow.ui.departments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cureflow.data.model.Department
import com.cureflow.data.model.Doctor
import com.cureflow.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class DepartmentsViewModel : ViewModel() {
    private val api = RetrofitClient.api

    var departments by mutableStateOf<List<Department>>(emptyList())
        private set

    var doctors by mutableStateOf<List<Doctor>>(emptyList())
        private set

    var selectedDepartmentId by mutableStateOf<String?>(null)
        var isLoading by mutableStateOf(false)
        private set

    init {
        loadDepartments()
        loadDoctors()
    }

    fun loadDepartments() {
        viewModelScope.launch {
            try {
                val response = api.getDepartments()
                if (response.isSuccessful && response.body() != null) {
                    departments = response.body()!!
                }
            } catch (_: Exception) {}
        }
    }

    fun loadDoctors(deptId: String? = null) {
        viewModelScope.launch {
            isLoading = true
            selectedDepartmentId = deptId
            try {
                val response = api.getDoctors(deptId)
                if (response.isSuccessful && response.body() != null) {
                    doctors = response.body()!!
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }
}
