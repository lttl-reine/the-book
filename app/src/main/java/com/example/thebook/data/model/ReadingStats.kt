package com.example.thebook.data.model

data class ReadingStats(
    val totalBooksRead: Int = 0,
    val totalBooksStarted: Int = 0,
    val totalPagesRead: Int = 0,
    val favoriteGenres: List<Pair<String, Int>> = emptyList()
)
