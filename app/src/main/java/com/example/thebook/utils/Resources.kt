package com.example.thebook.utils

sealed class Resources<T> {
    data class Success<T>(val data: T) : Resources<T>()
    data class Error<T>(val exception: Exception) : Resources<T>()
    data class Loading<T>(val data: T? = null) : Resources<T>()

}
