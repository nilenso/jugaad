package com.nilenso.jugaad.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class JugaadConfigurationState(
    val serverUrl: String = "",
    val authToken: String = "",
    val smsMatchString: String = "",
    val jugaadEnabled: Boolean = false
)

class ConfigurationViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(JugaadConfigurationState())
    val uiState: StateFlow<JugaadConfigurationState> = _uiState.asStateFlow()
}
