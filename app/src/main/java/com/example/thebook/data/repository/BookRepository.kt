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
                    }
                }
                trySend(Resources.Success(books))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resources.Error(error.toException()))
            }
        }

        booksRef.addValueEventListener(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing books listener")
            booksRef.removeEventListener(valueEventListener)
        }
    }

    fun getNewestBooks(limit: Int = 10): Flow<Resources<List<Book>>> = callbackFlow {
        trySend(Resources.Loading())

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                for (childSnapshot in snapshot.children) {
                    val book = childSnapshot.getValue(Book::class.java)
                    book?.let {
                        it.bookId = childSnapshot.key ?: ""
                        books.add(it)
                    }
                }
                // Sort by uploadDate descending and take the specified limit
                val newestBooks = books.sortedByDescending { it.uploadDate }.take(limit)
                trySend(Resources.Success(newestBooks))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resources.Error(error.toException()))
            }
        }

        booksRef.orderByChild("uploadDate").limitToLast(limit).addValueEventListener(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing newest books listener")
            booksRef.removeEventListener(valueEventListener)
        }
    }

    fun getPopularBooks(limit: Int = 10): Flow<Resources<List<Book>>> = callbackFlow {
        trySend(Resources.Loading())

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                for (childSnapshot in snapshot.children) {
                    val book = childSnapshot.getValue(Book::class.java)
                    book?.let {
                        it.bookId = childSnapshot.key ?: ""
                        books.add(it)
                    }
                }
                // Filter books with ratings, sort by averageRating descending, then totalRatings descending
                val popularBooks = books
                    .filter { it.averageRating > 0.0 }
                    .sortedWith(compareByDescending<Book> { it.averageRating }
                        .thenByDescending { it.totalRatings })
                    .take(limit)
                trySend(Resources.Success(popularBooks))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resources.Error(error.toException()))
            }
        }

        booksRef.addValueEventListener(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing popular books listener")
            booksRef.removeEventListener(valueEventListener)
        }
    }



    fun getBookById(bookId: String): Flow<Resources<Book>> = callbackFlow {
        trySend(Resources.Loading())

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val book = snapshot.getValue(Book::class.java)
                if (book != null) {
                    book.bookId = snapshot.key ?: "" // Ensure bookId is set
                    trySend(Resources.Success(book))
                } else {
                    trySend(Resources.Error(Exception("Book not found")))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resources.Error(error.toException()))
            }
        }

        booksRef.child(bookId).addValueEventListener(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing book by ID listener for $bookId")
            booksRef.child(bookId).removeEventListener(valueEventListener)
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

    // Function to update an existing book
    fun updateBook(bookId: String, updatedBook: Book): Flow<Resources<Void>> = callbackFlow {
        trySend(Resources.Loading())
        booksRef.child(bookId).setValue(updatedBook)
            .addOnSuccessListener {
                trySend(Resources.Success(bookId as Void))
            }
            .addOnFailureListener { e ->
                trySend(Resources.Error(e))
            }
        awaitClose { /* No specific cleanup needed */ }
    }

    // Function to delete a book
    fun deleteBook(bookId: String): Flow<Resources<Void>> = callbackFlow {
        trySend(Resources.Loading())
        booksRef.child(bookId).removeValue()
            .addOnSuccessListener {
                trySend(Resources.Success(bookId as Void))
            }
            .addOnFailureListener { e ->
                trySend(Resources.Error(e))
            }
        awaitClose { /* No specific cleanup needed */ }
    }

    fun updateBookPageCount(bookId: String, pageCount: Int): Flow<com.example.thebook.utils.Resources<Unit>> = callbackFlow {
        Log.d(TAG, "Updating pageCount for book ID: $bookId to $pageCount")
        trySend(com.example.thebook.utils.Resources.Loading())

        val bookRef = booksRef.child(bookId)
        bookRef.child("pageCount").setValue(pageCount)
            .addOnSuccessListener {
                Log.d(TAG, "pageCount updated successfully for book ID: $bookId")
                trySend(com.example.thebook.utils.Resources.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update pageCount for book ID: $bookId: ${e.message}", e)
                trySend(com.example.thebook.utils.Resources.Error(Exception(e.message ?: "Failed to update pageCount")))
                close()
            }
        awaitClose {
            Log.d(TAG, "Closing update pageCount operation for book ID: $bookId")
        }
    }
    // Add a new review for a book
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
            .limitToLast(limit)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reviews = mutableListOf<Review>()
                    for (childSnapshot in snapshot.children) {
                        val review = childSnapshot.getValue(Review::class.java)
                        review?.let {
                            it.reviewId = childSnapshot.key ?: ""
                            reviews.add(it)
                        }
                    }
                    callback(reviews.reversed(), null)
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

    // Get books by specific genre
    fun getBooksByGenre(genre: String): Flow<Resources<List<Book>>> = callbackFlow {
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
                trySend(Resources.Success(books))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resources.Error(error.toException()))
            }
        }

        booksRef.addValueEventListener(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing books by genre listener for $genre")
            booksRef.removeEventListener(valueEventListener)
        }
    }

    // Get search suggestions based on partial query
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