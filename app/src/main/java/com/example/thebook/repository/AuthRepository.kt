package com.example.thebook.repository

import com.example.thebook.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("Users")

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val snapshot = usersRef.child(firebaseUser.uid).get().await()
                val user = snapshot.getValue(User::class.java) ?: User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Login failed: User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, name: String) : Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
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
                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed: User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
}