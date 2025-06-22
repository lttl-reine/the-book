package com.example.thebook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.Book
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.ReadingProgressRepository
import com.example.thebook.utils.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val bookRepository  = BookRepository()
    private val readingProgressRepository = ReadingProgressRepository()

    private val _books = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val books : StateFlow<Resources<List<Book>>> = _books.asStateFlow()

    private val _recentlyReadBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val recentlyReadBooks: StateFlow<Resources<List<Book>>> = _recentlyReadBooks.asStateFlow()

    // Thêm StateFlow cho currently reading books
    private val _currentlyReadingBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val currentlyReadingBooks: StateFlow<Resources<List<Book>>> = _currentlyReadingBooks.asStateFlow()

    // Thêm StateFlow cho finished books
    private val _finishedBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val finishedBooks: StateFlow<Resources<List<Book>>> = _finishedBooks.asStateFlow()

    init {
        fetchBooks()
        loadRecentlyReadBooks() // Kích hoạt load recently read books
        loadCurrentlyReadingBooks()
        loadFinishedBooks()
    }

    private fun fetchBooks() {
        bookRepository.getBooks().onEach { result ->
            _books.value = result
        }.launchIn(viewModelScope)
    }

    private fun loadRecentlyReadBooks() {
        viewModelScope.launch {
            combine(
                readingProgressRepository.getReadingHistory(limit = 10), // Giới hạn 10 sách gần đây nhất
                bookRepository.getBooks()
            ) { readingHistoryResource, allBooksResource ->
                when {
                    readingHistoryResource is Resources.Success && allBooksResource is Resources.Success -> {
                        val recentlyReadProgress = readingHistoryResource.data ?: emptyList()
                        val allBooks = allBooksResource.data ?: emptyList()

                        // Lọc và sắp xếp các sách đã đọc gần đây
                        val recentlyReadBooksList = recentlyReadProgress
                            .filter { it.lastReadPage > 0 } // Chỉ lấy sách đã đọc ít nhất 1 trang
                            .mapNotNull { progress ->
                                allBooks.find { book -> book.bookId == progress.bookId }
                            }
                            .distinctBy { it.bookId } // Loại bỏ trùng lặp

                        Resources.Success(recentlyReadBooksList)
                    }
                    readingHistoryResource is Resources.Loading || allBooksResource is Resources.Loading -> {
                        Resources.Loading()
                    }
                    readingHistoryResource is Resources.Error -> {
                        Resources.Error(readingHistoryResource.exception ?: Exception("Error loading reading history"))
                    }
                    allBooksResource is Resources.Error -> {
                        Resources.Error(allBooksResource.exception ?: Exception("Error loading books"))
                    }
                    else -> {
                        Resources.Error(Exception("Unknown error"))
                    }
                }
            }.collect {
                _recentlyReadBooks.value = it
            }
        }
    }

    private fun loadCurrentlyReadingBooks() {
        viewModelScope.launch {
            combine(
                readingProgressRepository.getCurrentlyReading(),
                bookRepository.getBooks()
            ) { currentlyReadingResource, allBooksResource ->
                when {
                    currentlyReadingResource is Resources.Success && allBooksResource is Resources.Success -> {
                        val currentlyReadingProgress = currentlyReadingResource.data ?: emptyList()
                        val allBooks = allBooksResource.data ?: emptyList()

                        val currentlyReadingBooksList = currentlyReadingProgress.mapNotNull { progress ->
                            allBooks.find { book -> book.bookId == progress.bookId }
                        }

                        Resources.Success(currentlyReadingBooksList)
                    }
                    currentlyReadingResource is Resources.Loading || allBooksResource is Resources.Loading -> {
                        Resources.Loading()
                    }
                    currentlyReadingResource is Resources.Error -> {
                        Resources.Error(currentlyReadingResource.exception ?: Exception("Error loading currently reading"))
                    }
                    allBooksResource is Resources.Error -> {
                        Resources.Error(allBooksResource.exception ?: Exception("Error loading books"))
                    }
                    else -> {
                        Resources.Error(Exception("Unknown error"))
                    }
                }
            }.collect {
                _currentlyReadingBooks.value = it
            }
        }
    }

    private fun loadFinishedBooks() {
        viewModelScope.launch {
            combine(
                readingProgressRepository.getFinishedBooks(),
                bookRepository.getBooks()
            ) { finishedBooksResource, allBooksResource ->
                when {
                    finishedBooksResource is Resources.Success && allBooksResource is Resources.Success -> {
                        val finishedProgress = finishedBooksResource.data ?: emptyList()
                        val allBooks = allBooksResource.data ?: emptyList()

                        val finishedBooksList = finishedProgress.mapNotNull { progress ->
                            allBooks.find { book -> book.bookId == progress.bookId }
                        }

                        Resources.Success(finishedBooksList)
                    }
                    finishedBooksResource is Resources.Loading || allBooksResource is Resources.Loading -> {
                        Resources.Loading()
                    }
                    finishedBooksResource is Resources.Error -> {
                        Resources.Error(finishedBooksResource.exception ?: Exception("Error loading finished books"))
                    }
                    allBooksResource is Resources.Error -> {
                        Resources.Error(allBooksResource.exception ?: Exception("Error loading books"))
                    }
                    else -> {
                        Resources.Error(Exception("Unknown error"))
                    }
                }
            }.collect {
                _finishedBooks.value = it
            }
        }
    }

    // Thêm function để refresh data
    fun refreshData() {
        fetchBooks()
        loadRecentlyReadBooks()
        loadCurrentlyReadingBooks()
        loadFinishedBooks()
    }
}