package com.example.thebook.data.model

data class ReadingProgress(
    val userId: String = "",
    val bookId: String = "",
    val lastReadPage: Int = 0,
    val lastReadAt: Long = 0L,
    val isCompleted: Boolean = false
)
