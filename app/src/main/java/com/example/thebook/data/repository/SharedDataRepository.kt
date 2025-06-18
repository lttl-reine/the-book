package com.example.thebook.data.repository

import android.util.Log
import com.example.thebook.data.model.Category
import com.example.thebook.data.model.Language
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedDataRepository {
    private final var TAG: String = "SharedDateRepository"

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _languages = MutableStateFlow<List<Language>>(emptyList())
    val languages: StateFlow<List<Language>> = _languages

    init {
        fetchCategories()
        fetchLanguages()
    }

    private fun fetchCategories() {
        database.getReference("Category").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryList = mutableListOf<Category>()
                for (childSnapshot in snapshot.children) {
                    val category = childSnapshot.getValue(Category::class.java)
                    category?.let {
                        it.id = (childSnapshot.key ?: "").toString()
                        categoryList.add(it)

                        Log.d(TAG, "Fetched Category: ${it.displayName} (${it.name})")
                    } ?: Log.e(TAG, "Failed to parse category: ${childSnapshot.key}")
                }
                _categories.value = categoryList
                Log.d(TAG, "Categories fetched successfully: ${categoryList.size} items")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting categories from Realtime Database: ${error.message}", error.toException())
            }

        })
    }

    private fun fetchLanguages() {
        database.getReference("Language").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val languageList = mutableListOf<Language>()
                for (childSnapshot in snapshot.children) {
                    val language = childSnapshot.getValue(Language::class.java)
                    language?.let {
                        languageList.add(it)
                        Log.d(TAG, "Fetched Language: ${it.displayName} (${it.name})")
                    } ?: Log.e(TAG, "Failed to parse language: ${childSnapshot.key}")

                }
                _languages.value = languageList
                Log.d(TAG, "Languages fetched successfully: ${languageList.size} items")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error getting languages from Realtime Database: ${error.message}", error.toException())
            }

        })
    }
}