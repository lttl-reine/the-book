package com.example.thebook.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.Book
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.utils.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Thêm vào SearchViewModel.kt

class SearchViewModel : ViewModel() {

    private val bookRepository = BookRepository()

    private val _searchResults = MutableStateFlow<Resources<List<Book>>>(Resources.Success(emptyList()))
    val searchResults: StateFlow<Resources<List<Book>>> = _searchResults.asStateFlow()

    private val _suggestions = MutableStateFlow<Resources<List<String>>>(Resources.Success(emptyList()))
    val suggestions: StateFlow<Resources<List<String>>> = _suggestions.asStateFlow()

    private var currentQuery = ""
    private var currentGenre: String? = null

    fun searchBooks(query: String) {
        currentQuery = query
        viewModelScope.launch {
            bookRepository.searchBooks(query, currentGenre).collect { resource ->
                _searchResults.value = resource
            }
        }
    }

    // Thêm method mới để search với filter đặc biệt (newest, popular)
    fun searchBooksWithFilter(query: String, filter: String) {
        currentQuery = query
        viewModelScope.launch {
            when (filter) {
                "newest" -> {
                    // Gọi method lấy sách mới nhất từ repository
                    bookRepository.getNewestBooks().collect { resource ->
                        _searchResults.value = resource
                    }
                }
                "popular" -> {
                    // Gọi method lấy sách phổ biến từ repository
                    bookRepository.getPopularBooks().collect { resource ->
                        _searchResults.value = resource
                    }
                }
                else -> {
                    searchBooks(query)
                }
            }
        }
    }

    fun filterByGenre(genre: String) {
        currentGenre = genre
        viewModelScope.launch {
            bookRepository.searchBooks(currentQuery, genre).collect { resource ->
                _searchResults.value = resource
            }
        }
    }

    fun clearGenreFilter() {
        currentGenre = null
        searchBooks(currentQuery)
    }

    // Method để load tất cả sách (không giới hạn)
    fun loadAllBooks() {
        viewModelScope.launch {
            bookRepository.getBooks().collect { resource ->
                _searchResults.value = resource
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
}
