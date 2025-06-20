package com.example.thebook.data.repository

import android.util.Log
import com.example.thebook.data.model.User
import com.example.thebook.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val TAG: String = "AuthRepository"
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("Users")

    suspend fun login(email: String, password: String): Resource<User> {
        return try {
            Resource.Loading("Starting login for email")
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val snapshot = usersRef.child(firebaseUser.uid).get().await()
                val user = snapshot.getValue(User::class.java) ?: User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName
                )
                Log.d(TAG, "User data fetched: ${user.email}")
                Resource.Success(user)
            } else {
                Resource.Error(Exception("Login failed: User not found"))
            }
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }

    suspend fun register(email: String, password: String, name: String) : Resource<User> {
        return try {
            Log.d(TAG, "Starting registration for email: $email, name: $name")
            Resource.Loading("")
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                Log.d(TAG, "Registration successful")
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()


                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: " ",
                    name = firebaseUser.displayName
                )
                usersRef.child(firebaseUser.uid).setValue(user).await()
                Log.d(TAG, "User data saved successfully")
                Resource.Success(user)
            } else {
                Log.e(TAG, "Registration failed: User not found")
                Resource.Error(Exception("Registration failed: User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}", e)
            Resource.Error(e)
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun isUserLoggedIn() : Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser
        return firebaseUser?.let {
            val snapshot = usersRef.child(firebaseUser.uid).get().await()
            snapshot.getValue(User::class.java) ?: User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: ""
            )
        }
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
}