package com.example.thebook.data.model

data class Library(
    val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val readingStatus: String = "NOT_STARTED", // NOT_STARTED, READING, COMPLETED
    val lastReadAt: Long = 0L,
    val notes: String = ""
)

enum class ReadingStatus {
    NOT_STARTED("Chưa đọc"),
    READING("Đang đọc"),
    COMPLETED("Đã hoàn thành");

    constructor(displayName: String) {
        this.displayName = displayName
    }

    val displayName: String
}