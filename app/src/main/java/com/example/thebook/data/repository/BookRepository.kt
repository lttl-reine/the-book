package com.example.thebook.data.repository

import android.util.Log
import com.example.thebook.data.model.Book
import com.example.thebook.utils.Resource
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

    fun getBooks() : Flow<Resource<List<Book>>> = callbackFlow {
        trySend(Resource.Loading())

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
                trySend(Resource.Success(books))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch books: ${error.message}", error.toException())
                trySend(Resource.Error(error.toException()))
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

    fun getBookById(bookId: String) : Flow<Resource<Book>> = callbackFlow {
        Log.d(TAG, "Starting to fetch book with ID: $bookId")
        trySend(Resource.Loading())

        val bookRef = database.getReference("Books").child(bookId)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val book = snapshot.getValue(Book::class.java)
                if (book != null) {
                    book.bookId = snapshot.key ?: ""
                    Log.d(TAG, "Book fetched successfully: ${book.title}, ID: ${book.bookId}")
                    trySend(Resource.Success(book))
                } else {
                    Log.e(TAG, "Book not found for ID: $bookId")
                    trySend(Resource.Error(Exception("Book not found for ID: $bookId")))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch book with ID $bookId: ${error.message}", error.toException())
                trySend(Resource.Error(error.toException()))
            }
        }
        // Just liston one time
        bookRef.addListenerForSingleValueEvent(valueEventListener)

        awaitClose {
            Log.d(TAG, "Closing book listener for ID: $bookId")
        }
    }

    fun saveBook(book: Book, uploaderId: String): Flow<Resource<Unit>> = callbackFlow {
        Log.d(TAG, "Starting to save book: ${book.title}, uploaderId: $uploaderId")
        trySend(Resource.Loading())
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
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save book: ${e.message}", e)
                trySend(Resource.Error(Exception(e.message ?: "Failed to add book")))
                close()
            }
        awaitClose {
            Log.d(TAG, "Closing save book operation")
        }
    }
}