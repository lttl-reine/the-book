package com.example.thebook.data.model

data class Book(
    var bookId: String = "",
    val title: String = "",
    val author: String = "",
    val genre: List<String> = emptyList(),
    val description: String = "",
    val coverImageUrl: String = "",
    val bookFileUrl: String = "",
    val publishedYear: Int = 0,
    val language: String = "",
    val pageCount: Int = 0,
    val averageRating: Float = 0.0f,
    val totalRatings: Int = 0,
    var uploadDate: Long = 0L,
    val uploaderId: String = "",
    val isFree: Boolean = true,
    val price: Double = 0.0
)