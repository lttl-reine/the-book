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

    // All books
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

    // Newest books (limited to 10)
    private val _newestBooks = MutableStateFlow<Resources<List<Book>>>(Resources.Loading())
    val newestBooks: StateFlow<Resources<List<Book>>> = _newestBooks.asStateFlow()

    // Popular books (by rating, limited to 10)
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
        fetchBooks()
        loadRecentlyReadBooks()
        loadCurrentlyReadingBooks()
        loadFinishedBooks()
        loadGenreBooks() // Gọi hàm load sách theo thể loại
    }

    private fun fetchBooks() {
        // Cập nhật trạng thái loading trước khi bắt đầu fetch
        _isLoading.value = true
        bookRepository.getBooks().onEach { result ->
            _books.value = result
            // Khi có kết quả, cập nhật trạng thái loading và xử lý các danh mục sách
            if (result is Resources.Success) {
                _isLoading.value = false // Tắt loading khi dữ liệu đã sẵn sàng
                processNewestBooks(result.data ?: emptyList())
                processPopularBooks(result.data ?: emptyList())
            } else if (result is Resources.Error) {
                _isLoading.value = false // Tắt loading nếu có lỗi
                // Có thể xử lý lỗi cụ thể ở đây
            }
        }.launchIn(viewModelScope)
    }

    private fun processNewestBooks(allBooks: List<Book>) {
        val newestBooks = allBooks
            .sortedByDescending { it.uploadDate }
            .take(10) // Lấy 10 sách mới nhất
        _newestBooks.value = Resources.Success(newestBooks)
    }

    private fun processPopularBooks(allBooks: List<Book>) {
        val popularBooks = allBooks
            .filter { it.averageRating > 0.0 } // Chỉ lấy sách có đánh giá
            .sortedWith(compareByDescending<Book> { it.averageRating }
                .thenByDescending { it.totalRatings }) // Sắp xếp theo điểm trung bình, sau đó là tổng số lượt đánh giá
            .take(10) // Lấy 10 sách phổ biến nhất
        _popularBooks.value = Resources.Success(popularBooks)
    }

    private fun loadRecentlyReadBooks() {
        viewModelScope.launch {
            _isLoading.value = true // Bật loading
            combine(
                readingProgressRepository.getReadingHistory(limit = 10),
                bookRepository.getBooks()
            ) { readingHistoryResource, allBooksResource ->
                when {
                    readingHistoryResource is Resources.Success && allBooksResource is Resources.Success -> {
                        val recentlyReadProgress = readingHistoryResource.data ?: emptyList()
                        val allBooks = allBooksResource.data ?: emptyList()

                        val recentlyReadBooksList = recentlyReadProgress
                            .filter { it.lastReadPage > 0 }
                            .take(10) // Giới hạn 10 sách gần đây nhất
                            .mapNotNull { progress ->
                                allBooks.find { book -> book.bookId == progress.bookId }
                            }
                            .distinctBy { it.bookId } // Loại bỏ trùng lặp nếu có
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
                if (it !is Resources.Loading) _isLoading.value = false // Tắt loading khi hoàn tất
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
        // Có thể gọi lại fetchBooks() hoặc filter danh sách _books hiện có
        // để cập nhật UI dựa trên bộ lọc mới
    }

    // Function để thay đổi cách sắp xếp
    fun setSelectedSort(sort: String) {
        _selectedSort.value = sort
        // Tương tự, có thể sắp xếp lại danh sách _books hiện có
        // hoặc gọi lại fetchBooks() với tham số sắp xếp
    }

    // Thêm function để refresh data
    fun refreshData() {
        fetchBooks()
        loadRecentlyReadBooks()
        loadCurrentlyReadingBooks()
        loadFinishedBooks()
        loadGenreBooks() // Refresh cả sách theo thể loại
    }
}