package com.example.thebook.ui.reader

data class EnhancedTocItem(
    val title: String,
    val href: String,
    val index: Int,
    val isFromOriginalToc: Boolean = false
)