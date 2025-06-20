package com.example.thebook.data.model

import com.google.firebase.Timestamp

data class ReadingProgress(
    val userId: String,
    val bookId: String,
    val lastReadPage: Int,
    val lastReadAt: Timestamp,
    val isFinished: Boolean
)
