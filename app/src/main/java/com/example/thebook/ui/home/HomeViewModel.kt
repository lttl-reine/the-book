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
    private val bookRepository = BookRepository()
    private val readingProgressRepository = ReadingProgressRepository()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // All books (for filter/sort)
    private val _books = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val books: StateFlow<Resources<List<Book>>> = _books.asStateFlow()

    // Recently read books
    private val _recentlyReadBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val recentlyReadBooks: StateFlow<Resources<List<Book>>> = _recentlyReadBooks.asStateFlow()

    // Currently reading books
    private val _currentlyReadingBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val currentlyReadingBooks: StateFlow<Resources<List<Book>>> = _currentlyReadingBooks.asStateFlow()

    // Finished books
    private val _finishedBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val finishedBooks: StateFlow<Resources<List<Book>>> = _finishedBooks.asStateFlow()

    // Newest books
    private val _newestBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val newestBooks: StateFlow<Resources<List<Book>>> = _newestBooks.asStateFlow()

    // Popular books
    private val _popularBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val popularBooks: StateFlow<Resources<List<Book>>> = _popularBooks.asStateFlow()

    // Books by genre
    private val _fictionBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val fictionBooks: StateFlow<Resources<List<Book>>> = _fictionBooks.asStateFlow()

    private val _scienceBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val scienceBooks: StateFlow<Resources<List<Book>>> = _scienceBooks.asStateFlow()

    private val _historyBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val historyBooks: StateFlow<Resources<List<Book>>> = _historyBooks.asStateFlow()

    // Filter state
    private val _selectedFilter = MutableStateFlow("all")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Sort state
    private val _selectedSort = MutableStateFlow("title")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    init {
        // Khởi tạo các lần tải dữ liệu
        fetchBooks() // Vẫn cần fetch tất cả sách cho bộ lọc
        loadRecentlyReadBooks()
        loadCurrentlyReadingBooks()
        loadFinishedBooks()
        loadNewestBooks() // Gọi hàm mới từ Repository
        loadPopularBooks() // Gọi hàm mới từ Repository
        loadGenreBooks()
    }

    private fun fetchBooks() {
        _isLoading.value = true
        bookRepository.getBooks().onEach { result ->
            _books.value = result
            if (result !is Resources.Loading) _isLoading.value = false
        }.launchIn(viewModelScope)
    }

    private fun loadRecentlyReadBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            combine(
                readingProgressRepository.getReadingHistory(limit = 10),
                bookRepository.getBooks() // Lấy tất cả sách để tìm theo bookId
            ) { readingHistoryResource, allBooksResource ->
                when {
                    readingHistoryResource is Resources.Success && allBooksResource is Resources.Success -> {
                        val recentlyReadProgress = readingHistoryResource.data ?: emptyList()
                        val allBooks = allBooksResource.data ?: emptyList()

                        val recentlyReadBooksList = recentlyReadProgress
                            .filter { it.lastReadPage > 0 }
                            .take(10)
                            .mapNotNull { progress ->
                                allBooks.find { book -> book.bookId == progress.bookId }
                            }
                            .distinctBy { it.bookId }
                        Resources.Success(recentlyReadBooksList)
                    }
                    readingHistoryResource is Resources.Loading || allBooksResource is Resources.Loading -> {
                        Resources.Loading()
                    }
                    readingHistoryResource is Resources.Error -> {
                        Resources.Error(readingHistoryResource.exception ?: Exception("Error loading reading history"))
                    }
                    allBooksResource is Resources.Error -> {
                        Resources.Error(allBooksResource.exception ?: Exception("Error loading all books for recently read"))
                    }
                    else -> {
                        Resources.Error(Exception("Unknown error"))
                    }
                }
            }.collect {
                _recentlyReadBooks.value = it
                if (it !is Resources.Loading) _isLoading.value = false
            }
        }
    }

    private fun loadCurrentlyReadingBooks() {
        viewModelScope.launch {
            _isLoading.value = true
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
                if (it !is Resources.Loading) _isLoading.value = false
            }
        }
    }

    private fun loadFinishedBooks() {
        viewModelScope.launch {
            _isLoading.value = true
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
                if (it !is Resources.Loading) _isLoading.value = false
            }
        }
    }

    // Modified: Call getNewestBooks from BookRepository
    private fun loadNewestBooks() {
        _isLoading.value = true
        bookRepository.getNewestBooks().onEach { result ->
            _newestBooks.value = result
            if (result !is Resources.Loading) _isLoading.value = false
        }.launchIn(viewModelScope)
    }

    // Modified: Call getPopularBooks from BookRepository
    private fun loadPopularBooks() {
        _isLoading.value = true
        bookRepository.getPopularBooks().onEach { result ->
            _popularBooks.value = result
            if (result !is Resources.Loading) _isLoading.value = false
        }.launchIn(viewModelScope)
    }

    private fun loadGenreBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            // Tải sách thể loại Fiction
            bookRepository.getBooksByGenre("Fiction").onEach { result ->
                _fictionBooks.value = result
                if (result !is Resources.Loading) _isLoading.value = false
            }.launchIn(viewModelScope)

            // Tải sách thể loại Science
            bookRepository.getBooksByGenre("Science").onEach { result ->
                _scienceBooks.value = result
                if (result !is Resources.Loading) _isLoading.value = false
            }.launchIn(viewModelScope)

            // Tải sách thể loại History
            bookRepository.getBooksByGenre("History").onEach { result ->
                _historyBooks.value = result
                if (result !is Resources.Loading) _isLoading.value = false
            }.launchIn(viewModelScope)
        }
    }

    // Function để thay đổi bộ lọc
    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    // Function để thay đổi cách sắp xếp
    fun setSelectedSort(sort: String) {
        _selectedSort.value = sort
    }

    // Thêm function để refresh data
    fun refreshData() {
        fetchBooks()
        loadRecentlyReadBooks()
        loadCurrentlyReadingBooks()
        loadFinishedBooks()
        loadNewestBooks()
        loadPopularBooks()
        loadGenreBooks()
    }
}