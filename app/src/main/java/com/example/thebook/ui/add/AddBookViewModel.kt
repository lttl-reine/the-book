package com.example.thebook.ui.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.Book
import com.example.thebook.data.repository.AuthRepository
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.SharedDataRepository
import com.example.thebook.utils.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddBookViewModel(
    private val sharedDataRepository: SharedDataRepository,
    private val bookRepository: BookRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val TAG = "AddBookViewModel"

    // State of save book process
    private val _saveBookStatus = MutableStateFlow<SaveBookStatus>(SaveBookStatus.Idle)
    val saveBookStatus: StateFlow<SaveBookStatus> = _saveBookStatus

    // Category and language from SharedDataRepository
    val categories = sharedDataRepository.categories
    val languages = sharedDataRepository.languages

    fun saveBook(book : Book) {
        viewModelScope.launch {
            val uploaderId = authRepository.getCurrentUserId()
            if (uploaderId == null) {
                _saveBookStatus.value = SaveBookStatus.Error("User not logged in  or ID not found.")
                Log.d(TAG, "saveBook: User not logged in  or ID not found.")
                return@launch
            }
            bookRepository.saveBook(book, uploaderId).collectLatest { resource ->
                when (resource) {
                    is Resources.Error -> {
                        Log.e(TAG, "saveBook: Can't save book")
                        _saveBookStatus.value =
                            SaveBookStatus.Error((resource.exception ?: "Unknown error").toString())
                    }
                    is Resources.Loading -> {
                        Log.d(TAG, "saveBook: In progress saving book")
                        _saveBookStatus.value = SaveBookStatus.Loading
                    }
                    is Resources.Success -> {
                        Log.d(TAG, "saveBook: Book saved success")
                        _saveBookStatus.value = SaveBookStatus.Success("Book saved success")
                    }
                }
            }
        }
    }

    fun resetSaveBookStatus() {
        _saveBookStatus.value = SaveBookStatus.Idle
    }
}

sealed class SaveBookStatus {
    data object Idle : SaveBookStatus()
    data object Loading : SaveBookStatus()
    data class Success(val message: String) : SaveBookStatus()
    data class Error(val message: String) : SaveBookStatus()
}