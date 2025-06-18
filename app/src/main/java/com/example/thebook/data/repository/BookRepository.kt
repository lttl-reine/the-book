package com.example.thebook.data.repository

import com.example.thebook.data.model.Book
import com.example.thebook.util.Resource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BookRepository {
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
                    }
                }
                trySend(Resource.Success(books))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resource.Error(error.message))
            }
        }

        // Start to listen data
        booksRef.addValueEventListener(valueEventListener)

        // Callback to cancel listen when flow close
        awaitClose {
            booksRef.removeEventListener(valueEventListener)
        }

    }

    fun getBookById(bookId: String) : Flow<Resource<Book>> = callbackFlow {
        trySend(Resource.Loading())

        val bookRef = database.getReference("books").child(bookId)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val book = snapshot.getValue(Book::class.java)
                if (book != null) {
                    book.bookId = snapshot.key ?: ""
                    trySend(Resource.Success(book))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resource.Error(error.message))
            }
        }
        // Just liston one time
        bookRef.addListenerForSingleValueEvent(valueEventListener)
    }

    fun saveBook(book: Book, uploaderId: String): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading())
        val newBookRef = booksRef.push()
        val bookId = book.copy(bookId = newBookRef.key ?: "").toString()
        val uploadDate = System.currentTimeMillis()

        val bookToSave = book.copy(
            bookId = bookId,
            uploadDate = uploadDate,
            uploaderId = uploaderId
        )

        newBookRef.setValue(bookToSave)
            .addOnSuccessListener {
                trySend(Resource.Success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message ?: "Failed to add book"))
                close()
            }
        awaitClose {}
    }
}