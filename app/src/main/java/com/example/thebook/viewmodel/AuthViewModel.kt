package com.example.thebook.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.model.User
import com.example.thebook.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // Auth State
    private val _authState = MutableLiveData<Result<User>>()
    val authState : LiveData<Result<User>> = _authState

    // Current User
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = authRepository.login(email, password)
        }
    }

    fun register(email: String, password: String, confirmPassword : String, name : String) {
        if (password != confirmPassword) {
            _authState.value = Result.failure(Exception("Passowords do not match"))
            return
        }
        viewModelScope.launch {
            _authState.value = authRepository.register(email, password, name)
        }
    }

    fun logout() {
        authRepository.logout()
    }
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    fun fetchCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUser()
        }
    }
}

