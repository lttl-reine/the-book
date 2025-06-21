package com.example.thebook.data.model

data class Review(
    var reviewId: String = "",
    val bookId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0.0f,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)