package com.example.thebook.utils

sealed class Resource<T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val exception: Exception) : Resource<T>()
    data class Loading<T>(val data: T? = null) : Resource<T>()
}