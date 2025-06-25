package com.example.thebook.data.repository

import android.util.Log
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Library
import com.example.thebook.data.model.ReadingProgress
import com.example.thebook.data.model.ReadingStatus
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LibraryRepository {
    private val TAG = "LibraryRepository"
    private val database = FirebaseDatabase.getInstance()
    private val libraryRef = database.getReference("Library")
    private val booksRef = database.getReference("Books")
    private val readingProgressRef = database.getReference("ReadingProgress")

    // Thêm sách vào thư viện
    suspend fun addBookToLibrary(userId: String, bookId: String): Result<String> {
        return try {
            // Kiểm tra xem sách đã có trong thư viện chưa
            val exists = isBookInLibrary(userId, bookId)
            if (exists) {
                return Result.failure(Exception("Sách đã có trong thư viện"))
            }

            val libraryId = libraryRef.child(userId).push().key
                ?: return Result.failure(Exception("Không thể tạo ID cho thư viện"))

            val libraryItem = Library(
                id = libraryId,
                userId = userId,
                bookId = bookId,
                addedAt = System.currentTimeMillis(),
                readingStatus = ReadingStatus.NOT_STARTED.name
            )

            libraryRef.child(userId).child(libraryId).setValue(libraryItem).await()
            Log.d(TAG, "Book added to library successfully")
            Result.success(libraryId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding book to library", e)
            Result.failure(e)
        }
    }

    // Xóa sách khỏi thư viện
    suspend fun removeBookFromLibrary(userId: String, bookId: String): Result<Unit> {
        return try {
            val query = libraryRef.child(userId).orderByChild("bookId").equalTo(bookId)
            val snapshot = query.get().await()

            for (child in snapshot.children) {
                child.ref.removeValue().await()
            }

            Log.d(TAG, "Book removed from library successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing book from library", e)
            Result.failure(e)
        }
    }

    // Kiểm tra sách có trong thư viện không
    suspend fun isBookInLibrary(userId: String, bookId: String): Boolean {
        return try {
            val query = libraryRef.child(userId).orderByChild("bookId").equalTo(bookId)
            val snapshot = query.get().await()
            snapshot.exists() && snapshot.childrenCount > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if book is in library", e)
            false
        }
    }

    // Lấy danh sách sách trong thư viện
    suspend fun getUserLibrary(userId: String): Result<List<Pair<Library, Book>>> {
        return try {
            val librarySnapshot = libraryRef.child(userId).get().await()
            val libraryBooks = mutableListOf<Pair<Library, Book>>()

            for (libraryChild in librarySnapshot.children) {
                val libraryItem = libraryChild.getValue(Library::class.java)
                libraryItem?.let { library ->
                    // Lấy thông tin sách
                    val bookSnapshot = booksRef.child(library.bookId).get().await()
                    val book = bookSnapshot.getValue(Book::class.java)
                    book?.let {
                        libraryBooks.add(Pair(library, it))
                    }
                }
            }

            // Sắp xếp theo addedAt (giảm dần)
            val sortedBooks = libraryBooks.sortedByDescending { it.first.addedAt }

            Log.d(TAG, "Retrieved ${sortedBooks.size} books from library")
            Result.success(sortedBooks)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user library", e)
            Result.failure(e)
        }
    }

    // Cập nhật trạng thái đọc
    suspend fun updateReadingStatus(userId: String, bookId: String, status: ReadingStatus): Result<Unit> {
        return try {
            val query = libraryRef.child(userId).orderByChild("bookId").equalTo(bookId)
            val snapshot = query.get().await()

            for (child in snapshot.children) {
                child.ref.updateChildren(
                    mapOf(
                        "readingStatus" to status.name,
                        "lastReadAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            Log.d(TAG, "Reading status updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating reading status", e)
            Result.failure(e)
        }
    }

    // Cập nhật trạng thái yêu thích
    suspend fun updateFavoriteStatus(userId: String, bookId: String, isFavorite: Boolean): Result<Unit> {
        return try {
            val query = libraryRef.child(userId).orderByChild("bookId").equalTo(bookId)
            val snapshot = query.get().await()

            for (child in snapshot.children) {
                child.ref.updateChildren(mapOf("isFavorite" to isFavorite)).await()
            }

            Log.d(TAG, "Favorite status updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating favorite status", e)
            Result.failure(e)
        }
    }

    // Lấy tiến độ đọc
    suspend fun getReadingProgress(userId: String, bookId: String): ReadingProgress? {
        return try {
            val query = readingProgressRef.child(userId).orderByChild("bookId").equalTo(bookId)
            val snapshot = query.get().await()

            if (snapshot.exists()) {
                snapshot.children.first().getValue(ReadingProgress::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reading progress", e)
            null
        }
    }

    // Lọc thư viện theo trạng thái
    suspend fun getLibraryByStatus(userId: String, status: ReadingStatus): Result<List<Pair<Library, Book>>> {
        return try {
            val librarySnapshot = libraryRef.child(userId).orderByChild("readingStatus").equalTo(status.name).get().await()
            val libraryBooks = mutableListOf<Pair<Library, Book>>()

            for (libraryChild in librarySnapshot.children) {
                val libraryItem = libraryChild.getValue(Library::class.java)
                libraryItem?.let { library ->
                    val bookSnapshot = booksRef.child(library.bookId).get().await()
                    val book = bookSnapshot.getValue(Book::class.java)
                    book?.let {
                        libraryBooks.add(Pair(library, it))
                    }
                }
            }

            // Sắp xếp theo lastReadAt (giảm dần)
            val sortedBooks = libraryBooks.sortedByDescending { it.first.lastReadAt ?: 0 }

            Result.success(sortedBooks)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting library by status", e)
            Result.failure(e)
        }
    }

    // Lấy sách yêu thích
    suspend fun getFavoriteBooks(userId: String): Result<List<Pair<Library, Book>>> {
        return try {
            val librarySnapshot = libraryRef.child(userId).orderByChild("isFavorite").equalTo(true).get().await()
            val favoriteBooks = mutableListOf<Pair<Library, Book>>()

            for (libraryChild in librarySnapshot.children) {
                val libraryItem = libraryChild.getValue(Library::class.java)
                libraryItem?.let { library ->
                    val bookSnapshot = booksRef.child(library.bookId).get().await()
                    val book = bookSnapshot.getValue(Book::class.java)
                    book?.let {
                        favoriteBooks.add(Pair(library, it))
                    }
                }
            }

            // Sắp xếp theo addedAt (giảm dần)
            val sortedBooks = favoriteBooks.sortedByDescending { it.first.addedAt }

            Result.success(sortedBooks)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting favorite books", e)
            Result.failure(e)
        }
    }
}