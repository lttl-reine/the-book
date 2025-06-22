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

    fun getSuggestions(query: String) {
        viewModelScope.launch {
            bookRepository.getSearchSuggestions(query).collect { resource ->
                _suggestions.value = resource
            }
        }
    }
}
