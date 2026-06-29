package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiComparisonService
import com.example.database.Comparison
import com.example.database.ComparisonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CompareUiState {
    object Idle : CompareUiState
    data class Loading(val stage: String) : CompareUiState
    data class Success(val comparison: Comparison) : CompareUiState
    data class Error(val message: String) : CompareUiState
}

class ComparisonViewModel(private val repository: ComparisonRepository) : ViewModel() {
    private val _compareUiState = MutableStateFlow<CompareUiState>(CompareUiState.Idle)
    val compareUiState: StateFlow<CompareUiState> = _compareUiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites

    val history: StateFlow<List<Comparison>> = combine(
        repository.allComparisons,
        _searchQuery,
        _showOnlyFavorites
    ) { list, query, favOnly ->
        var filtered = list
        if (favOnly) {
            filtered = filtered.filter { it.isFavorite }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.itemA.contains(query, ignoreCase = true) ||
                it.itemB.contains(query, ignoreCase = true) ||
                it.context.contains(query, ignoreCase = true) ||
                it.verdict.contains(query, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }

    fun compare(itemA: String, itemB: String, context: String, criteria: String = "") {
        if (itemA.isBlank() || itemB.isBlank()) {
            _compareUiState.value = CompareUiState.Error("Please enter both options to compare.")
            return
        }

        viewModelScope.launch {
            _compareUiState.value = CompareUiState.Loading("Initializing evaluation...")
            try {
                _compareUiState.value = CompareUiState.Loading("Consulting Gemini AI...")
                val result = GeminiComparisonService.performComparison(itemA, itemB, context, criteria)
                
                _compareUiState.value = CompareUiState.Loading("Saving to history...")
                val newId = repository.insertComparison(result)
                val savedComparison = result.copy(id = newId)
                
                _compareUiState.value = CompareUiState.Success(savedComparison)
            } catch (e: Exception) {
                _compareUiState.value = CompareUiState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun selectComparison(comparison: Comparison) {
        _compareUiState.value = CompareUiState.Success(comparison)
    }

    fun resetToForm() {
        _compareUiState.value = CompareUiState.Idle
    }

    fun toggleFavorite(comparison: Comparison) {
        viewModelScope.launch {
            repository.updateFavorite(comparison.id, !comparison.isFavorite)
            val currentState = _compareUiState.value
            if (currentState is CompareUiState.Success && currentState.comparison.id == comparison.id) {
                _compareUiState.value = CompareUiState.Success(currentState.comparison.copy(isFavorite = !comparison.isFavorite))
            }
        }
    }

    fun deleteComparison(id: Long) {
        viewModelScope.launch {
            repository.deleteComparisonById(id)
            val currentState = _compareUiState.value
            if (currentState is CompareUiState.Success && currentState.comparison.id == id) {
                _compareUiState.value = CompareUiState.Idle
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAllComparisons()
        }
    }
}

class ComparisonViewModelFactory(private val repository: ComparisonRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComparisonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ComparisonViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
