package com.example.thebook.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.User
import com.example.thebook.data.repository.AuthRepository
import com.example.thebook.utils.Resource
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel"
    private val authRepository = AuthRepository()

    // Auth State
    private val _authState = MutableLiveData<Resource<User>>()
    val authState : LiveData<Resource<User>> = _authState

    // Current User
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    fun login(email: String, password: String) {
        Log.d(TAG, "Initiating login for email: $email")
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            _authState.value = authRepository.login(email, password)
        }
    }

    fun register(email: String, password: String, confirmPassword : String, name : String) {
        if (password != confirmPassword) {
            Log.e(TAG, "Registration failed: Passwords do not match")
            _authState.value = Resource.Error(Exception("Passwords do not match"))
            return
        }
        Log.d(TAG, "Initiating registration for email: $email, name: $name")
        viewModelScope.launch {
            _authState.value = authRepository.register(email, password, name)
        }
    }

    fun logout() {
        Log.d(TAG, "Initiating logout")
        authRepository.logout()
    }
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = authRepository.isUserLoggedIn()
        Log.d(TAG, "User logged in status: $isLoggedIn")
        return isLoggedIn
    }

    fun fetchCurrentUser() {
        Log.d(TAG, "Fetching current user")
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUser()
        }
    }
}

