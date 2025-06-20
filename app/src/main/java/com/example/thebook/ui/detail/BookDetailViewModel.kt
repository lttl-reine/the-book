package com.example.thebook.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Category
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.SharedDataRepository
import com.example.thebook.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val sharedDataRepository: SharedDataRepository,
) : ViewModel() {
    private val TAG  = "BookDetailViewModel"

    private val _book = MutableStateFlow<Resource<Book>>(Resource.Loading())
    val book : StateFlow<Resource<Book>> = _book.asStateFlow()

    val categories: StateFlow<List<Category>> = sharedDataRepository.categories

    fun loadBook(bookId: String) {
        if (bookId.isNotEmpty()) {
            Log.d(TAG, "Loading book with ID: $bookId")
            fetchBookDetails(bookId)
        } else {
            Log.d(TAG, "Invalid bookId")
            _book.value = Resource.Error(Exception("Invalid book ID"))
        }
    }


    private fun fetchBookDetails(bookId : String) {
        bookRepository.getBookById(bookId).onEach { result ->
            _book.value = result
        }.launchIn(viewModelScope)
    }

}