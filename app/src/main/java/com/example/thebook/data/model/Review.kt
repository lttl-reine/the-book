package com.example.thebook.data.model

import com.google.firebase.Timestamp

data class Review(
    val bookId: String,
    val userId: String,
    val rating: Double,
    val comment: String,
    val timestamp: Timestamp
)
