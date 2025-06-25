package com.example.thebook.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.thebook.data.repository.LibraryRepository
import com.example.thebook.data.repository.ReadingProgressRepository

class LibraryViewModelFactory(
    private val libraryRepository: LibraryRepository,
    private val readingProgressRepository: ReadingProgressRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(libraryRepository, readingProgressRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}