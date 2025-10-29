package com.example.thebook.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.Book
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.PaginatedResult
import com.example.thebook.utils.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false
)

class SearchViewModel : ViewModel() {

    private val bookRepository = BookRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _suggestions = MutableStateFlow<Resources<List<String>>>(Resources.Success(emptyList()))
    val suggestions: StateFlow<Resources<List<String>>> = _suggestions.asStateFlow()

    private var currentQuery = ""
    private var currentGenre: String? = null
    private var currentFilter: String? = null

    fun searchBooks(query: String, resetPagination: Boolean = true) {
        currentQuery = query
        currentFilter = null

        if (resetPagination) {
            _uiState.value = _uiState.value.copy(
                books = emptyList(),
                currentPage = 1,
                isLoading = true,
                error = null
            )
        }

        performSearch(query, currentGenre, _uiState.value.currentPage)
    }

    fun searchBooksWithFilter(query: String, filter: String, resetPagination: Boolean = true) {
        currentQuery = query
        currentFilter = filter

        if (resetPagination) {
            _uiState.value = _uiState.value.copy(
                books = emptyList(),
                currentPage = 1,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            when (filter) {
                "newest" -> {
                    bookRepository.getNewestBooks().collect { resource ->
                        handleNonPaginatedResults(resource)
                    }
                }
                "popular" -> {
                    bookRepository.getPopularBooks().collect { resource ->
                        handleNonPaginatedResults(resource)
                    }
                }
                else -> {
                    performSearch(query, currentGenre, _uiState.value.currentPage)
                }
            }
        }
    }

    fun filterByGenre(genre: String, resetPagination: Boolean = true) {
        currentGenre = genre

        if (resetPagination) {
            _uiState.value = _uiState.value.copy(
                books = emptyList(),
                currentPage = 1,
                isLoading = true,
                error = null
            )
        }

        performGenreSearch(genre, _uiState.value.currentPage)
    }

    fun clearGenreFilter() {
        currentGenre = null
        searchBooks(currentQuery, resetPagination = true)
    }

    fun loadAllBooks(resetPagination: Boolean = true) {
        currentQuery = ""
        currentGenre = null
        currentFilter = null

        if (resetPagination) {
            _uiState.value = _uiState.value.copy(
                books = emptyList(),
                currentPage = 1,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            bookRepository.getBooks().collect { resource ->
                handleNonPaginatedResults(resource)
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore) return

        _uiState.value = currentState.copy(isLoadingMore = true)
        val nextPage = currentState.currentPage + 1

        when {
            currentFilter != null -> {
                // For special filters like newest/popular, we don't support pagination
                // since they use different methods
                return
            }
            currentGenre != null -> {
                performGenreSearch(currentGenre!!, nextPage)
            }
            else -> {
                performSearch(currentQuery, null, nextPage)
            }
        }
    }

    private fun performSearch(query: String, genre: String?, page: Int) {
        viewModelScope.launch {
            bookRepository.searchBooksPaginated(query, genre, page).collect { resource ->
                handlePaginatedResults(resource, page)
            }
        }
    }

    private fun performGenreSearch(genre: String, page: Int) {
        viewModelScope.launch {
            bookRepository.getBooksByGenrePaginated(genre, page).collect { resource ->
                handlePaginatedResults(resource, page)
            }
        }
    }

    private fun handlePaginatedResults(resource: Resources<PaginatedResult<Book>>, page: Int) {
        val currentState = _uiState.value

        when (resource) {
            is Resources.Loading -> {
                if (page == 1) {
                    _uiState.value = currentState.copy(isLoading = true, error = null)
                } else {
                    _uiState.value = currentState.copy(isLoadingMore = true, error = null)
                }
            }
            is Resources.Success -> {
                val paginatedResult = resource.data
                val newBooks = if (page == 1) {
                    paginatedResult.data
                } else {
                    currentState.books + paginatedResult.data
                }

                _uiState.value = currentState.copy(
                    books = newBooks,
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    hasMore = paginatedResult.hasMore,
                    totalCount = paginatedResult.totalCount,
                    currentPage = paginatedResult.currentPage
                )
            }
            is Resources.Error -> {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = resource.exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun handleNonPaginatedResults(resource: Resources<List<Book>>) {
        val currentState = _uiState.value

        when (resource) {
            is Resources.Loading -> {
                _uiState.value = currentState.copy(isLoading = true, error = null)
            }
            is Resources.Success -> {
                _uiState.value = currentState.copy(
                    books = resource.data,
                    isLoading = false,
                    error = null,
                    hasMore = false, // Non-paginated results don't have "load more"
                    totalCount = resource.data.size,
                    currentPage = 1
                )
            }
            is Resources.Error -> {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = resource.exception.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun getSuggestions(query: String) {
        viewModelScope.launch {
            bookRepository.getSearchSuggestions(query).collect { resource ->
                _suggestions.value = resource
            }
        }
    }

    fun retryLastSearch() {
        val currentState = _uiState.value
        when {
            currentFilter != null -> {
                searchBooksWithFilter(currentQuery, currentFilter!!, resetPagination = true)
            }
            currentGenre != null -> {
                filterByGenre(currentGenre!!, resetPagination = true)
            }
            currentQuery.isNotEmpty() -> {
                searchBooks(currentQuery, resetPagination = true)
            }
            else -> {
                loadAllBooks(resetPagination = true)
            }
        }
    }
}