package com.example.thebook.data.model

import com.google.firebase.Timestamp

data class ReadingProgress(
    val userId: String = "",
    val bookId: String = "",
    val lastReadPage: Int = 0,
    val lastReadAt: Long = 0L,
    val isFinished: Boolean = false
)
