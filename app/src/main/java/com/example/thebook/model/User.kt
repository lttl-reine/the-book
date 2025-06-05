package com.example.thebook.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String? = null,
    val phoneNUmber: String? = null,
    val profileImage: String? = null,
    val userType: String? = "user",
    val createAt: Long = System.currentTimeMillis()
)
