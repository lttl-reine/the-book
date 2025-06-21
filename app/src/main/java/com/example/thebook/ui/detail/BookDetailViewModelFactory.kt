package com.example.thebook.ui.detail

import android.os.Bundle
import   androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.SharedDataRepository

class BookDetailViewModelFactory(
    private val bookRepository: BookRepository,
    private val sharedDataRepository: SharedDataRepository, // Add this
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(BookDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookDetailViewModel(bookRepository, sharedDataRepository) as T // Pass sharedDataRepository
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}