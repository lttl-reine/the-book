package com.example.thebook.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Library
import com.example.thebook.data.model.ReadingStatus
import com.example.thebook.data.repository.LibraryRepository
import com.example.thebook.data.repository.ReadingProgressRepository
import com.example.thebook.utils.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val readingProgressRepository: ReadingProgressRepository
) : ViewModel() {

    private val _libraryBooks = MutableStateFlow<Resources<List<LibraryItem>>>(Resources.Loading())
    val libraryBooks: StateFlow<Resources<List<LibraryItem>>> = _libraryBooks

    private val _addToLibraryResult = MutableStateFlow<Resources<String>?>(null)
    val addToLibraryResult: StateFlow<Resources<String>?> = _addToLibraryResult

    private val _removeFromLibraryResult = MutableStateFlow<Resources<Unit>?>(null)
    val removeFromLibraryResult: StateFlow<Resources<Unit>?> = _removeFromLibraryResult

    fun loadUserLibrary(userId: String) {
        viewModelScope.launch {
            _libraryBooks.value = Resources.Loading()

            val result = libraryRepository.getUserLibrary(userId)

            if (result.isSuccess) {
                val libraryData = result.getOrNull() ?: emptyList()
                val libraryItems = libraryData.map { (library, book) ->
                    // Get reading progress for each book
                    val progress = libraryRepository.getReadingProgress(userId, book.bookId)
                    LibraryItem(library, book, progress)
                }
                _libraryBooks.value = Resources.Success(libraryItems)
            } else {
                _libraryBooks.value = Resources.Error(result.exceptionOrNull() as Exception)
            }
        }
    }

    fun loadLibraryByStatus(userId: String, status: ReadingStatus) {
        viewModelScope.launch {
            _libraryBooks.value = Resources.Loading()

            val result = libraryRepository.getLibraryByStatus(userId, status)

            if (result.isSuccess) {
                val libraryData = result.getOrNull() ?: emptyList()
                val libraryItems = libraryData.map { (library, book) ->
                    val progress = libraryRepository.getReadingProgress(userId, book.bookId)
                    LibraryItem(library, book, progress)
                }
                _libraryBooks.value = Resources.Success(libraryItems)
            } else {
                _libraryBooks.value = Resources.Error(result.exceptionOrNull() as Exception)
            }
        }
    }

    fun loadFavoriteBooks(userId: String) {
        viewModelScope.launch {
            _libraryBooks.value = Resources.Loading()

            val result = libraryRepository.getFavoriteBooks(userId)

            if (result.isSuccess) {
                val favoriteData = result.getOrNull() ?: emptyList()
                val libraryItems = favoriteData.map { (library, book) ->
                    val progress = libraryRepository.getReadingProgress(userId, book.bookId)
                    LibraryItem(library, book, progress)
                }
                _libraryBooks.value = Resources.Success(libraryItems)
            } else {
                _libraryBooks.value = Resources.Error(result.exceptionOrNull() as Exception)
            }
        }
    }

    fun addBookToLibrary(userId: String, bookId: String) {
        viewModelScope.launch {
            _addToLibraryResult.value = Resources.Loading()

            val result = libraryRepository.addBookToLibrary(userId, bookId)

            if (result.isSuccess) {
                _addToLibraryResult.value = Resources.Success(result.getOrNull() ?: "")
            } else {
                _addToLibraryResult.value = Resources.Error(result.exceptionOrNull() as Exception)
            }
        }
    }

    fun removeBookFromLibrary(userId: String, bookId: String) {
        viewModelScope.launch {
            _removeFromLibraryResult.value = Resources.Loading()

            val result = libraryRepository.removeBookFromLibrary(userId, bookId)

            if (result.isSuccess) {
                _removeFromLibraryResult.value = Resources.Success(Unit)
            } else {
                _removeFromLibraryResult.value = Resources.Error(result.exceptionOrNull() as Exception)
            }
        }
    }

    private suspend fun loadLibraryWithProgress(libraryBooks: List<Pair<Library, Book>>) {
        try {
            val libraryItems = mutableListOf<LibraryItem>()

            libraryBooks.forEach { (library, book) ->
                // Get reading progress for each book
                readingProgressRepository.getReadingProgress(book.bookId)
                    .catch { e ->
                        // If error getting progress, add item without progress
                        libraryItems.add(LibraryItem(library, book, null))
                    }
                    .collect { progressResource ->
                        when (progressResource) {
                            is Resources.Success -> {
                                libraryItems.add(LibraryItem(library, book, progressResource.data))
                            }
                            is Resources.Error -> {
                                libraryItems.add(LibraryItem(library, book, null))
                            }
                            is Resources.Loading -> {
                                // Continue with existing item if loading
                            }
                        }
                    }
            }

            _libraryBooks.value = Resources.Success(libraryItems)
        } catch (e: Exception) {
            _libraryBooks.value = Resources.Error(e)
        }
    }

    fun updateReadingStatus(userId: String, bookId: String, status: ReadingStatus) {
        viewModelScope.launch {
            libraryRepository.updateReadingStatus(userId, bookId, status)
        }
    }

    fun updateFavoriteStatus(userId: String, bookId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            libraryRepository.updateFavoriteStatus(userId, bookId, isFavorite)
        }
    }

    suspend fun isBookInLibrary(userId: String, bookId: String): Boolean {
        return libraryRepository.isBookInLibrary(userId, bookId)
    }

    fun clearAddToLibraryResult() {
        _addToLibraryResult.value = null
    }

    fun clearRemoveFromLibraryResult() {
        _removeFromLibraryResult.value = null
    }
}