package com.example.thebook.data.repository

import android.util.Log
import com.example.thebook.data.model.ReadingProgress
import com.example.thebook.data.model.ReadingStats
import com.example.thebook.utils.Resources
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReadingProgressRepository {
    private val TAG = "ReadingProgressRepository"
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val readingProgressRef: DatabaseReference = database.getReference("ReadingProgress")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Save or update reading progress for a book
     */
    suspend fun saveReadingProgress(
        bookId: String,
        lastReadPage: Int,
        isFinished: Boolean = false
    ): Resources<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not logged in")
                return Resources.Error(Exception("User not logged in"))
            }

            val userId = currentUser.uid
            val progressId = "${userId}_${bookId}"

            val progress = ReadingProgress(
                userId = userId,
                bookId = bookId,
                lastReadPage = lastReadPage,
                lastReadAt = System.currentTimeMillis(),
                isFinished = isFinished
            )

            readingProgressRef.child(progressId).setValue(progress).await()
            Log.d(TAG, "Reading progress saved: bookId=$bookId, page=$lastReadPage, finished=$isFinished")
            Resources.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving reading progress: ${e.message}", e)
            Resources.Error(e)
        }
    }

    /**
     * Get reading progress for a specific book
     */
    fun getReadingProgress(bookId: String): Flow<Resources<ReadingProgress?>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(Resources.Error(Exception("User not logged in")))
            close()
            return@callbackFlow
        }

        trySend(Resources.Loading())

        val userId = currentUser.uid
        val progressId = "${userId}_${bookId}"

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val progress = snapshot.getValue(ReadingProgress::class.java)
                Log.d(TAG, "Reading progress fetched: $progress")
                trySend(Resources.Success(progress))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch reading progress: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }

        readingProgressRef.child(progressId).addValueEventListener(valueEventListener)

        awaitClose {
            readingProgressRef.child(progressId).removeEventListener(valueEventListener)
            Log.d(TAG, "Closing reading progress listener for bookId: $bookId")
        }
    }

    /**
     * Get reading history for current user
     */
    fun getReadingHistory(limit: Int = 20): Flow<Resources<List<ReadingProgress>>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(Resources.Error(Exception("User not logged in")))
            close()
            return@callbackFlow
        }

        trySend(Resources.Loading())
        val userId = currentUser.uid

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val progressList = mutableListOf<ReadingProgress>()

                for (childSnapshot in snapshot.children) {
                    val progress = childSnapshot.getValue(ReadingProgress::class.java)
                    progress?.let {
                        if (it.userId == userId) {
                            progressList.add(it)
                        }
                    }
                }

                // Sort by last read time (most recent first)
                val sortedProgress = progressList.sortedByDescending { it.lastReadAt }
                    .take(limit)

                Log.d(TAG, "Reading history fetched: ${sortedProgress.size} items")
                trySend(Resources.Success(sortedProgress))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch reading history: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }

        readingProgressRef
            .orderByChild("lastReadAt")
            .addValueEventListener(valueEventListener)

        awaitClose {
            readingProgressRef.removeEventListener(valueEventListener)
            Log.d(TAG, "Closing reading history listener")
        }
    }

    /**
     * Get currently reading books (not finished)
     */
    fun getCurrentlyReading(): Flow<Resources<List<ReadingProgress>>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(Resources.Error(Exception("User not logged in")))
            close()
            return@callbackFlow
        }

        trySend(Resources.Loading())
        val userId = currentUser.uid

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentlyReading = mutableListOf<ReadingProgress>()

                for (childSnapshot in snapshot.children) {
                    val progress = childSnapshot.getValue(ReadingProgress::class.java)
                    progress?.let {
                        if (it.userId == userId && !it.isFinished && it.lastReadPage > 0) {
                            currentlyReading.add(it)
                        }
                    }
                }

                // Sort by last read time (most recent first)
                val sortedProgress = currentlyReading.sortedByDescending { it.lastReadAt }

                Log.d(TAG, "Currently reading books fetched: ${sortedProgress.size} items")
                trySend(Resources.Success(sortedProgress))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch currently reading books: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }

        readingProgressRef.addValueEventListener(valueEventListener)

        awaitClose {
            readingProgressRef.removeEventListener(valueEventListener)
            Log.d(TAG, "Closing currently reading listener")
        }
    }

    /**
     * Get finished books
     */
    fun getFinishedBooks(): Flow<Resources<List<ReadingProgress>>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(Resources.Error(Exception("User not logged in")))
            close()
            return@callbackFlow
        }

        trySend(Resources.Loading())
        val userId = currentUser.uid

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val finishedBooks = mutableListOf<ReadingProgress>()

                for (childSnapshot in snapshot.children) {
                    val progress = childSnapshot.getValue(ReadingProgress::class.java)
                    progress?.let {
                        if (it.userId == userId && it.isFinished) {
                            finishedBooks.add(it)
                        }
                    }
                }

                // Sort by completion time (most recent first)
                val sortedProgress = finishedBooks.sortedByDescending { it.lastReadAt }

                Log.d(TAG, "Finished books fetched: ${sortedProgress.size} items")
                trySend(Resources.Success(sortedProgress))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch finished books: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }

        readingProgressRef.addValueEventListener(valueEventListener)

        awaitClose {
            readingProgressRef.removeEventListener(valueEventListener)
            Log.d(TAG, "Closing finished books listener")
        }
    }

    /**
     * Delete reading progress (when user wants to restart a book)
     */
    suspend fun deleteReadingProgress(bookId: String): Resources<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Resources.Error(Exception("User not logged in"))
            }

            val userId = currentUser.uid
            val progressId = "${userId}_${bookId}"

            readingProgressRef.child(progressId).removeValue().await()
            Log.d(TAG, "Reading progress deleted for bookId: $bookId")
            Resources.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reading progress: ${e.message}", e)
            Resources.Error(e)
        }
    }

    /**
     * Mark book as finished
     */
    suspend fun markBookAsFinished(bookId: String, lastPage: Int): Resources<Unit> {
        return saveReadingProgress(bookId, lastPage, true)
    }

    /**
     * Get reading statistics
     */
    fun getReadingStats(): Flow<Resources<ReadingStats>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(Resources.Error(Exception("User not logged in")))
            close()
            return@callbackFlow
        }

        trySend(Resources.Loading())
        val userId = currentUser.uid

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalBooksRead = 0
                var totalBooksStarted = 0
                var totalPagesRead = 0
                val genreCount = mutableMapOf<String, Int>()

                for (childSnapshot in snapshot.children) {
                    val progress = childSnapshot.getValue(ReadingProgress::class.java)
                    progress?.let {
                        if (it.userId == userId) {
                            if (it.lastReadPage > 0) {
                                totalBooksStarted++
                                totalPagesRead += it.lastReadPage
                            }
                            if (it.isFinished) {
                                totalBooksRead++
                            }
                        }
                    }
                }

                val stats = ReadingStats(
                    totalBooksRead = totalBooksRead,
                    totalBooksStarted = totalBooksStarted,
                    totalPagesRead = totalPagesRead,
                    favoriteGenres = genreCount.toList().sortedByDescending { it.second }.take(5)
                )

                Log.d(TAG, "Reading stats calculated: $stats")
                trySend(Resources.Success(stats))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch reading stats: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }

        readingProgressRef.addValueEventListener(valueEventListener)

        awaitClose {
            readingProgressRef.removeEventListener(valueEventListener)
            Log.d(TAG, "Closing reading stats listener")
        }
    }
}