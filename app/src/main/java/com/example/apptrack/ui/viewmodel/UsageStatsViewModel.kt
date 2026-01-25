package com.example.apptrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apptrack.data.UsageStats
import com.example.apptrack.data.UsageStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class UsageStatsViewModel(
    private val repository: UsageStatsRepository
) : ViewModel() {

    private val _usageStats = MutableStateFlow<UsageStats?>(null)
    val usageStats: StateFlow<UsageStats?> = _usageStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    val hasPermission: Boolean
        get() = repository.hasUsageStatsPermission()

    init {
        loadUsageStats()
    }

    fun loadUsageStats(date: Calendar = _selectedDate.value) {
        if (!hasPermission) {
            _error.value = "Usage Stats permission is required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _selectedDate.value = date
                _usageStats.value = repository.getUsageStatsForDate(date)
            } catch (e: Exception) {
                _error.value = "Failed to load usage stats: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadUsageStats(_selectedDate.value)
    }
}
