package com.example.thebook.data.repository

import android.util.Log
import com.example.thebook.data.model.User
import com.example.thebook.utils.Resources
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val TAG: String = "AuthRepository"
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("Users")

    suspend fun login(email: String, password: String): Resources<User> {
        return try {
            Resources.Loading("Starting login for email")
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
                Resources.Success(user)
            } else {
                Resources.Error(Exception("Login failed: User not found"))
            }
        } catch (e: Exception) {
            Resources.Error(e)
        }
    }

    suspend fun register(email: String, password: String, name: String) : Resources<User> {
        return try {
            Log.d(TAG, "Starting registration for email: $email, name: $name")
            Resources.Loading("")
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
                Resources.Success(user)
            } else {
                Log.e(TAG, "Registration failed: User not found")
                Resources.Error(Exception("Registration failed: User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}", e)
            Resources.Error(e)
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

    fun getUserName(userId: String, callback: (String?) -> Unit) {
        usersRef.child(userId).child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.getValue(String::class.java)
                callback(userName)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to get user name for $userId: ${error.message}", error.toException())
                callback(null)
            }
        })
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    fun getCurrentUserType(userId: String, callback: (String?) -> Unit) {
        usersRef.child(userId).child("userType").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userType = snapshot.getValue(String::class.java)
                callback(userType)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to get user type for $userId: ${error.message}", error.toException())
                callback(null)
            }
        })
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent to $email")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send password reset email to $email: ${e.message}", e)
            throw e
        }
    }
}