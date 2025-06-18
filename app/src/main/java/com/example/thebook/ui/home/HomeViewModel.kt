package com.example.thebook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.Book
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HomeViewModel : ViewModel() {
    private val bookRepository  = BookRepository()

    private val _books = MutableStateFlow<Resource<List<Book>>>(Resource.Loading())
    val books : StateFlow<Resource<List<Book>>> = _books.asStateFlow()

    init {
        fetchBooks()
    }

    private fun fetchBooks() {
        bookRepository.getBooks().onEach {
                result ->
            _books.value = result
        }.launchIn(viewModelScope)
    }
}