package com.example.thebook.data.model

import android.os.Parcelable
import com.google.firebase.database.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    var bookId: String = "",
    var title: String = "",
    var author: String = "",
    var genre: List<String> = emptyList(),
    var description: String = "",
    var coverImageUrl: String = "",
    var bookFileUrl: String = "",
    var publishedYear: Int = 0,
    var language: String = "",
    var pageCount: Int = 0,
    var averageRating: Float = 0.0f,
    var totalRatings: Int = 0,
    var uploadDate: Long = 0L,
    var uploaderId: String = "",
    var free: Boolean = true,
    var price: Double = 0.0
) : Parcelable