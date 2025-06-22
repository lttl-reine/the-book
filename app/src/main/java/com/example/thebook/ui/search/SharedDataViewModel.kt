package com.example.thebook.ui.search

import androidx.lifecycle.ViewModel
import com.example.thebook.data.model.Category
import com.example.thebook.data.model.Language
import com.example.thebook.data.repository.SharedDataRepository
import kotlinx.coroutines.flow.StateFlow

class SharedDataViewModel : ViewModel() {

    private val sharedDataRepository = SharedDataRepository()

    val categories: StateFlow<List<Category>> = sharedDataRepository.categories

    val languages: StateFlow<List<Language>> = sharedDataRepository.languages
}