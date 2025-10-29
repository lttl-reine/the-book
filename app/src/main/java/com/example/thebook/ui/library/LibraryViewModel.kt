package com.example.thebook.ui.library

import android.util.Log // Import Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.ReadingStatus
import com.example.thebook.data.repository.LibraryRepository
import com.example.thebook.data.repository.ReadingProgressRepository
import com.example.thebook.utils.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val readingProgressRepository: ReadingProgressRepository
) : ViewModel() {

    private val TAG = "LibraryViewModel" // Thêm TAG cho log

    private val _libraryBooks = MutableStateFlow<Resources<List<LibraryItem>>>(Resources.Loading())
    val libraryBooks: StateFlow<Resources<List<LibraryItem>>> = _libraryBooks

    private val _addToLibraryResult = MutableStateFlow<Resources<String>?>(null)
    val addToLibraryResult: StateFlow<Resources<String>?> = _addToLibraryResult

    private val _removeFromLibraryResult = MutableStateFlow<Resources<Unit>?>(null)
    val removeFromLibraryResult: StateFlow<Resources<Unit>?> = _removeFromLibraryResult

    private val _isBookInLibrary = MutableStateFlow<Boolean>(false)
    val isBookInLibrary: StateFlow<Boolean> = _isBookInLibrary

    fun loadUserLibrary(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "loadUserLibrary called for userId: $userId") // Log khi hàm được gọi
            _libraryBooks.value = Resources.Loading()

            val result = libraryRepository.getUserLibrary(userId)

            if (result.isSuccess) {
                val libraryData = result.getOrNull() ?: emptyList()
                val libraryItems = libraryData.map { (library, book) ->
                    // Get reading progress for each book
                    Log.d(TAG, "Fetching reading progress for bookId: ${book.bookId}, userId: $userId") // Log trước khi gọi getReadingProgress
                    val progress = libraryRepository.getReadingProgress(userId, book.bookId)
                    Log.d(TAG, "Reading progress for ${book.title} (ID: ${book.bookId}): $progress") // Log kết quả lấy progress
                    val item = LibraryItem(library, book, progress)
                    Log.d(TAG, "Created LibraryItem: $item") // Log LibraryItem được tạo
                    item
                }
                _libraryBooks.value = Resources.Success(libraryItems)
                Log.d(TAG, "loadUserLibrary: Successfully loaded ${libraryItems.size} books with progress.") // Log khi thành công
            } else {
                _libraryBooks.value = Resources.Error(result.exceptionOrNull() as Exception)
                Log.e(TAG, "loadUserLibrary: Error loading library: ${result.exceptionOrNull()?.message}", result.exceptionOrNull()) // Log khi lỗi
            }
        }
    }

    fun loadLibraryByStatus(userId: String, status: ReadingStatus) {
        viewModelScope.launch {
            Log.d(TAG, "loadLibraryByStatus called for userId: $userId, status: $status") // Log khi hàm được gọi
            _libraryBooks.value = Resources.Loading()

            val result = libraryRepository.getLibraryByStatus(userId, status)

            if (result.isSuccess) {
                val libraryData = result.getOrNull() ?: emptyList()
                val libraryItems = libraryData.map { (library, book) ->
                    Log.d(TAG, "Fetching reading progress for bookId: ${book.bookId}, userId: $userId (by status)") // Log trước khi gọi getReadingProgress
                    val progress = libraryRepository.getReadingProgress(userId, book.bookId)
                    Log.d(TAG, "Reading progress for ${book.title} (ID: ${book.bookId}): $progress") // Log kết quả lấy progress
                    val item = LibraryItem(library, book, progress)
                    Log.d(TAG, "Created LibraryItem: $item") // Log LibraryItem được tạo
                    item
                }
                _libraryBooks.value = Resources.Success(libraryItems)
                Log.d(TAG, "loadLibraryByStatus: Successfully loaded ${libraryItems.size} books by status.") // Log khi thành công
            } else {
                _libraryBooks.value = Resources.Error(result.exceptionOrNull() as Exception)
                Log.e(TAG, "loadLibraryByStatus: Error loading library by status: ${result.exceptionOrNull()?.message}", result.exceptionOrNull()) // Log khi lỗi
            }
        }
    }

    fun loadFavoriteBooks(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "loadFavoriteBooks called for userId: $userId") // Log khi hàm được gọi
            _libraryBooks.value = Resources.Loading()

            val result = libraryRepository.getFavoriteBooks(userId)

            if (result.isSuccess) {
                val favoriteData = result.getOrNull() ?: emptyList()
                val libraryItems = favoriteData.map { (library, book) ->
                    Log.d(TAG, "Fetching reading progress for bookId: ${book.bookId}, userId: $userId (favorites)") // Log trước khi gọi getReadingProgress
                    val progress = libraryRepository.getReadingProgress(userId, book.bookId)
                    Log.d(TAG, "Reading progress for ${book.title} (ID: ${book.bookId}): $progress") // Log kết quả lấy progress
                    val item = LibraryItem(library, book, progress)
                    Log.d(TAG, "Created LibraryItem: $item") // Log LibraryItem được tạo
                    item
                }
                _libraryBooks.value = Resources.Success(libraryItems)
                Log.d(TAG, "loadFavoriteBooks: Successfully loaded ${libraryItems.size} favorite books.") // Log khi thành công
            } else {
                _libraryBooks.value = Resources.Error(result.exceptionOrNull() as Exception)
                Log.e(TAG, "loadFavoriteBooks: Error loading favorite books: ${result.exceptionOrNull()?.message}", result.exceptionOrNull()) // Log khi lỗi
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

    fun checkBookInLibrary(userId: String, bookId: String) {
        viewModelScope.launch {
            val exists = libraryRepository.isBookInLibrary(userId, bookId)
            _isBookInLibrary.value = exists
            Log.d(TAG, "checkBookInLibrary: Book $bookId for user $userId exists: $exists")
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