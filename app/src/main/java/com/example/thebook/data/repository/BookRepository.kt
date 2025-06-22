package com.example.thebook.data.repository

import android.util.Log
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Review
import com.example.thebook.utils.Resources
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class BookRepository {
    private val TAG = "BookRepository"
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val booksRef: DatabaseReference = database.getReference("Books")
    private val reviewsRef: DatabaseReference = database.getReference("Reviews") // Using Realtime Database for reviews

    fun getBooks() : Flow<Resources<List<Book>>> = callbackFlow {
        trySend(Resources.Loading())

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                for (childSnapshot in snapshot.children) {
                    val book = childSnapshot.getValue(Book::class.java)
                    book?.let {
                        it.bookId = childSnapshot.key ?: ""
                        books.add(it)
                        Log.d(TAG, "Book added: ${it.title}, ID: ${it.bookId}")
                    }
                }
                Log.d(TAG, "Fetched ${books.size} books successfully")
                trySend(Resources.Success(books))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch books: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }

        // Start to listen data
        booksRef.addValueEventListener(valueEventListener)

        // Callback to cancel listen when flow close
        awaitClose {
            Log.d(TAG, "Closing books listener")
            booksRef.removeEventListener(valueEventListener)
        }
    }

    fun getBookById(bookId: String) : Flow<Resources<Book>> = callbackFlow {
        Log.d(TAG, "Starting to fetch book with ID: $bookId")
        trySend(Resources.Loading())

        val bookRef = database.getReference("Books").child(bookId)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val book = snapshot.getValue(Book::class.java)
                if (book != null) {
                    book.bookId = snapshot.key ?: ""
                    Log.d(TAG, "Book fetched successfully: ${book.title}, ID: ${book.bookId}")
                    trySend(Resources.Success(book))
                } else {
                    Log.e(TAG, "Book not found for ID: $bookId")
                    trySend(Resources.Error(Exception("Book not found for ID: $bookId")))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch book with ID $bookId: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }
        // Just listen one time
        bookRef.addListenerForSingleValueEvent(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing book listener for ID: $bookId")
        }
    }

    fun saveBook(book: Book, uploaderId: String): Flow<Resources<Unit>> = callbackFlow {
        Log.d(TAG, "Starting to save book: ${book.title}, uploaderId: $uploaderId")
        trySend(Resources.Loading())
        val newBookRef = booksRef.push()
        val generatedBookId = newBookRef.key ?: UUID.randomUUID().toString()
        val uploadDate = System.currentTimeMillis()

        val bookToSave = book.copy(
            bookId = generatedBookId,
            uploadDate = uploadDate,
            uploaderId = uploaderId
        )

        newBookRef.setValue(bookToSave)
            .addOnSuccessListener {
                Log.d(TAG, "Book saved successfully: ${bookToSave.title}, ID: $generatedBookId")
                trySend(Resources.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save book: ${e.message}", e)
                trySend(Resources.Error(Exception(e.message ?: "Failed to add book")))
                close()
            }
        awaitClose {
            Log.d(TAG, "Closing save book operation")
        }
    }

    /**
     * Add a new review for a book
     */
    fun addReview(review: Review, callback: (Boolean, String?) -> Unit) {
        val newReviewRef = reviewsRef.push()
        val reviewWithId = review.copy(reviewId = newReviewRef.key ?: UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis())
        newReviewRef.setValue(reviewWithId)
            .addOnSuccessListener {
                updateBookRating(review.bookId) { success, error ->
                    callback(success, error)
                }
            }
            .addOnFailureListener { exception ->
                callback(false, exception.message)
            }
    }

    /**
     * Get reviews for a specific book
     */
    fun getReviewsForBook(
        bookId: String,
        limit: Int = 10,
        callback: (List<Review>?, String?) -> Unit
    ) {
        reviewsRef
            .orderByChild("bookId")
            .equalTo(bookId)
            .limitToLast(limit) // Realtime Database uses limitToLast for descending order
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reviews = mutableListOf<Review>()
                    for (childSnapshot in snapshot.children) {
                        val review = childSnapshot.getValue(Review::class.java)
                        review?.let {
                            it.reviewId = childSnapshot.key ?: "" // Set the reviewId from the key
                            reviews.add(it)
                        }
                    }
                    // Since limitToLast gets the "last" (most recent if ordered by timestamp), reverse if needed
                    // to match DESCENDING order visually, but timestamp sorting isn't direct in queries.
                    // You might need to sort in-memory if strict timestamp DESC is required.
                    callback(reviews.reversed(), null) // Reverse to get most recent first if pushed by timestamp
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null, error.message)
                }
            })
    }

    /**
     * Check if user has already reviewed this book
     */
    fun hasUserReviewed(
        bookId: String,
        userId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        reviewsRef
            .orderByChild("bookId")
            .equalTo(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasReviewed = false
                    for (childSnapshot in snapshot.children) {
                        val review = childSnapshot.getValue(Review::class.java)
                        if (review?.userId == userId) {
                            hasReviewed = true
                            break
                        }
                    }
                    callback(hasReviewed, null)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, error.message)
                }
            })
    }

    /**
     * Update book's average rating and total ratings count
     */
    private fun updateBookRating(bookId: String, callback: (Boolean, String?) -> Unit) {
        reviewsRef
            .orderByChild("bookId")
            .equalTo(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reviews = mutableListOf<Review>()
                    for (childSnapshot in snapshot.children) {
                        val review = childSnapshot.getValue(Review::class.java)
                        review?.let { reviews.add(it) }
                    }

                    if (reviews.isNotEmpty()) {
                        val averageRating = reviews.map { it.rating }.average()
                        val totalRatings = reviews.size

                        // Update book document in the 'Books' node
                        val bookRef = database.getReference("Books").child(bookId)
                        bookRef.updateChildren(
                            mapOf(
                                "averageRating" to averageRating,
                                "totalRatings" to totalRatings
                            )
                        ).addOnSuccessListener {
                            callback(true, null)
                        }.addOnFailureListener { exception ->
                            callback(false, exception.message)
                        }
                    } else {
                        // If no reviews, set ratings to default
                        val bookRef = database.getReference("Books").child(bookId)
                        bookRef.updateChildren(
                            mapOf(
                                "averageRating" to 0.0,
                                "totalRatings" to 0
                            )
                        ).addOnSuccessListener {
                            callback(true, null)
                        }.addOnFailureListener { exception ->
                            callback(false, exception.message)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, error.message)
                }
            })
    }

    /**
     * Delete a review (for moderation or user request)
     */
    fun deleteReview(
        bookId: String,
        userId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        reviewsRef
            .orderByChild("bookId")
            .equalTo(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var reviewFound = false
                    for (childSnapshot in snapshot.children) {
                        val review = childSnapshot.getValue(Review::class.java)
                        if (review?.userId == userId) {
                            childSnapshot.ref.removeValue()
                                .addOnSuccessListener {
                                    updateBookRating(bookId) { success, error ->
                                        callback(success, error)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    callback(false, exception.message)
                                }
                            reviewFound = true
                            break
                        }
                    }
                    if (!reviewFound) {
                        callback(false, "Review not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false, error.message)
                }
            })
    }

    fun searchBooks(
        query: String,
        genre: String? = null,
        limit: Int = 20
    ): Flow<Resources<List<Book>>> = callbackFlow {
        Log.d(TAG, "Starting search with query: '$query', genre: '$genre'")
        trySend(Resources.Loading())

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                val searchQuery = query.lowercase().trim()

                for (childSnapshot in snapshot.children) {
                    val book = childSnapshot.getValue(Book::class.java)
                    book?.let {
                        it.bookId = childSnapshot.key ?: ""

                        // Check if book matches search criteria
                        val matchesQuery = if (searchQuery.isEmpty()) {
                            true
                        } else {
                            it.title.lowercase().contains(searchQuery) ||
                                    it.author.lowercase().contains(searchQuery) ||
                                    it.genre.any { genre -> genre.lowercase().contains(searchQuery) }
                        }

                        val matchesGenre = genre?.let { filterGenre ->
                            it.genre.any { bookGenre ->
                                bookGenre.lowercase() == filterGenre.lowercase()
                            }
                        } ?: true

                        if (matchesQuery && matchesGenre) {
                            books.add(it)
                        }
                    }
                }

                // Sort by relevance (exact matches first, then partial matches)
                val sortedBooks = books.sortedWith(compareBy<Book> { book ->
                    when {
                        book.title.lowercase() == searchQuery -> 0
                        book.author.lowercase() == searchQuery -> 1
                        book.title.lowercase().startsWith(searchQuery) -> 2
                        book.author.lowercase().startsWith(searchQuery) -> 3
                        else -> 4
                    }
                }.thenByDescending { it.averageRating })

                val limitedBooks = sortedBooks.take(limit)
                Log.d(TAG, "Search completed: ${limitedBooks.size} books found")
                trySend(Resources.Success(limitedBooks))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Search failed: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }

        booksRef.addListenerForSingleValueEvent(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing search listener")
        }
    }

    /**
     * Get books by specific genre
     */
    fun getBooksByGenre(genre: String): Flow<Resources<List<Book>>> = callbackFlow {
        Log.d(TAG, "Fetching books by genre: $genre")
        trySend(Resources.Loading())

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()

                for (childSnapshot in snapshot.children) {
                    val book = childSnapshot.getValue(Book::class.java)
                    book?.let {
                        it.bookId = childSnapshot.key ?: ""
                        if (it.genre.any { bookGenre ->
                                bookGenre.lowercase() == genre.lowercase()
                            }) {
                            books.add(it)
                        }
                    }
                }

                // Sort by rating and upload date
                val sortedBooks = books.sortedWith(
                    compareByDescending<Book> { it.averageRating }
                        .thenByDescending { it.uploadDate }
                )

                Log.d(TAG, "Found ${sortedBooks.size} books for genre: $genre")
                trySend(Resources.Success(sortedBooks))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch books by genre: ${error.message}", error.toException())
                trySend(Resources.Error(error.toException()))
            }
        }

        booksRef.addListenerForSingleValueEvent(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing genre search listener")
        }
    }

    /**
     * Get search suggestions based on partial query
     */
    fun getSearchSuggestions(query: String, limit: Int = 5): Flow<Resources<List<String>>> = callbackFlow {
        if (query.length < 2) {
            trySend(Resources.Success(emptyList()))
            close()
            return@callbackFlow
        }

        trySend(Resources.Loading())
        val searchQuery = query.lowercase().trim()

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val suggestions = mutableSetOf<String>()

                for (childSnapshot in snapshot.children) {
                    val book = childSnapshot.getValue(Book::class.java)
                    book?.let {
                        // Add title suggestions
                        if (it.title.lowercase().contains(searchQuery)) {
                            suggestions.add(it.title)
                        }

                        // Add author suggestions
                        if (it.author.lowercase().contains(searchQuery)) {
                            suggestions.add(it.author)
                        }

                        // Add genre suggestions
                        it.genre.forEach { genre ->
                            if (genre.lowercase().contains(searchQuery)) {
                                suggestions.add(genre)
                            }
                        }
                    }
                }

                val limitedSuggestions = suggestions.take(limit)
                trySend(Resources.Success(limitedSuggestions))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resources.Error(error.toException()))
            }
        }

        booksRef.addListenerForSingleValueEvent(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing suggestions listener")
        }
    }

}