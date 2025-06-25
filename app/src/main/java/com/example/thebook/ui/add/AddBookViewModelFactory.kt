package com.example.thebook.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.thebook.data.repository.AuthRepository
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.SharedDataRepository

class AddBookViewModelFactory(
    private val sharedDataRepository: SharedDataRepository,
    private val bookRepository: BookRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddBookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddBookViewModel(sharedDataRepository, bookRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}