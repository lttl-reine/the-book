package com.example.thebook.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thebook.data.model.ReadingStatus
import com.example.thebook.data.model.User
import com.example.thebook.data.repository.AuthRepository
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.LibraryRepository
import com.example.thebook.data.repository.ReadingProgressRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val libraryRepository = LibraryRepository()
    private val bookRepository = BookRepository()
    private val readingProgressRepository = ReadingProgressRepository()

    private val TAG = "ProfileViewModel"

    // LiveData for UI
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _readingStats = MutableLiveData<ReadingStatistics>()
    val readingStats: LiveData<ReadingStatistics> = _readingStats

    private val _readingPreferences = MutableLiveData<List<String>>()
    val readingPreferences: LiveData<List<String>> = _readingPreferences

    private val _readingHabitData = MutableLiveData<List<MonthlyReadingData>>()
    val readingHabitData: LiveData<List<MonthlyReadingData>> = _readingHabitData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val user = authRepository.getCurrentUser()
                _currentUser.value = user
                Log.d(TAG, "User profile loaded: ${user?.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
                _errorMessage.value = "Failed to load user profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadReadingStatistics() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _errorMessage.value = "User not logged in"
                    return@launch
                }

                _isLoading.value = true

                // Get user's library
                val libraryResult = libraryRepository.getUserLibrary(userId)
                libraryResult.fold(
                    onSuccess = { libraryItems ->
                        // Calculate statistics
                        val totalBooks = libraryItems.size
                        val completedBooks = libraryItems.filter {
                            it.library.readingStatus == ReadingStatus.FINISHED.name
                        }

                        // Calculate total reading time from reading progress
//                        var totalReadingTime = 0L
//                        libraryItems.forEach { item ->
//                            item.progress?.let { progress ->
//                                totalReadingTime += progress.totalReadingTimeMinutes
//                            }
//                        }

                        // Find favorite genre
                        val genreCount = mutableMapOf<String, Int>()
                        libraryItems.forEach { item ->
                            item.book.genre.forEach { genre ->
                                genreCount[genre] = genreCount.getOrDefault(genre, 0) + 1
                            }
                        }
                        val favoriteGenre = genreCount.maxByOrNull { it.value }?.key

                        // Books read this month
                        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val booksThisMonth = completedBooks.count { item ->
                            item.library.lastReadAt?.let { timestamp ->
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                cal.get(Calendar.MONTH) == currentMonth &&
                                        cal.get(Calendar.YEAR) == currentYear
                            } ?: false
                        }

                        // Calculate average rating of read books
                        val ratedBooks = completedBooks.mapNotNull { it.book.averageRating }
                        val averageRating = if (ratedBooks.isNotEmpty()) {
                            ratedBooks.average()
                        } else 0.0

                        val stats = ReadingStatistics(
                            totalBooksRead = completedBooks.size,
                            //totalReadingTimeMinutes = totalReadingTime,
                            favoriteGenre = favoriteGenre,
                            booksReadThisMonth = booksThisMonth,
                            averageRating = averageRating,
                            readingStreak = calculateReadingStreak(completedBooks.map { it.library })
                        )

                        _readingStats.value = stats
                        Log.d(TAG, "Reading statistics loaded: $stats")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error loading reading statistics", exception)
                        _errorMessage.value = "Failed to load reading statistics: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadReadingStatistics", e)
                _errorMessage.value = "Failed to load statistics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadReadingPreferences() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch

                // Get user's library and analyze preferred genres
                val libraryResult = libraryRepository.getUserLibrary(userId)
                libraryResult.fold(
                    onSuccess = { libraryItems ->
                        val genrePreferences = mutableSetOf<String>()

                        // Add genres from user's library
                        libraryItems.forEach { item ->
                            genrePreferences.addAll(item.book.genre)
                        }

                        // If no preferences found, set default ones
                        if (genrePreferences.isEmpty()) {
                            genrePreferences.addAll(listOf("Non-fiction", "Self-help", "History"))
                        }

                        _readingPreferences.value = genrePreferences.toList()
                        Log.d(TAG, "Reading preferences loaded: $genrePreferences")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error loading reading preferences", exception)
                        // Set default preferences on error
                        _readingPreferences.value = listOf("Non-fiction", "Self-help", "History")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadReadingPreferences", e)
                _readingPreferences.value = listOf("Non-fiction", "Self-help", "History")
            }
        }
    }

    fun loadReadingHabitData() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch

                val libraryResult = libraryRepository.getUserLibrary(userId)
                libraryResult.fold(
                    onSuccess = { libraryItems ->
                        val habitData = calculateMonthlyReadingData(libraryItems)
                        _readingHabitData.value = habitData
                        Log.d(TAG, "Reading habit data loaded: ${habitData.size} months")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error loading reading habit data", exception)
                        _readingHabitData.value = generateDefaultHabitData()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadReadingHabitData", e)
                _readingHabitData.value = generateDefaultHabitData()
            }
        }
    }

    fun updateReadingPreferences(preferences: List<String>) {
        viewModelScope.launch {
            try {
                // In a real app, you might want to save these preferences to Firebase
                // For now, just update the local state
                _readingPreferences.value = preferences
                Log.d(TAG, "Reading preferences updated: $preferences")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating reading preferences", e)
                _errorMessage.value = "Failed to update preferences: ${e.message}"
            }
        }
    }

    fun logout() {
        try {
            authRepository.logout()
            Log.d(TAG, "User logged out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            _errorMessage.value = "Logout failed: ${e.message}"
        }
    }

    private fun calculateReadingStreak(libraryItems: List<com.example.thebook.data.model.Library>): Int {
        val completedBooks = libraryItems
            .filter { it.readingStatus == ReadingStatus.FINISHED.name }
            .sortedByDescending { it.lastReadAt ?: 0 }

        if (completedBooks.isEmpty()) return 0

        var streak = 0
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var currentDay = today

        for (book in completedBooks) {
            val bookDate = book.lastReadAt ?: continue
            val bookCalendar = Calendar.getInstance().apply {
                timeInMillis = bookDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (bookCalendar.timeInMillis == currentDay) {
                streak++
                currentDay -= 24 * 60 * 60 * 1000 // Go back one day
            } else if (bookCalendar.timeInMillis < currentDay) {
                break // Gap in reading streak
            }
        }

        return streak
    }

    private fun calculateMonthlyReadingData(libraryItems: List<com.example.thebook.ui.library.LibraryItem>): List<MonthlyReadingData> {
        val monthlyData = mutableMapOf<String, MonthlyReadingData>()
        val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Initialize last 6 months with zero data
        for (i in 5 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            val monthKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
            val monthName = dateFormat.format(calendar.time)

            monthlyData[monthKey] = MonthlyReadingData(
                month = calendar.get(Calendar.MONTH),
                monthName = monthName,
                booksRead = 0,
                totalPages = 0,
                readingTimeMinutes = 0L
            )
        }

        // Fill with actual reading data
        libraryItems.forEach { item ->
            if (item.library.readingStatus == ReadingStatus.FINISHED.name) {
                item.library.lastReadAt?.let { timestamp ->
                    calendar.timeInMillis = timestamp
                    val monthKey = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"

                    monthlyData[monthKey]?.let { existing ->
                        monthlyData[monthKey] = existing.copy(
                            booksRead = existing.booksRead + 1,
                            totalPages = existing.totalPages + (item.book.pageCount ?: 0)
//                            readingTimeMinutes = existing.readingTimeMinutes +
//                                    (item.progress?.totalReadingTimeMinutes ?: 0L)
                        )
                    }
                }
            }
        }

        return monthlyData.values.sortedBy { it.month }
    }

    private fun generateDefaultHabitData(): List<MonthlyReadingData> {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
        val defaultData = listOf(2, 3, 1, 4, 2, 3) // Sample data

        return months.mapIndexed { index, month ->
            MonthlyReadingData(
                month = index,
                monthName = month,
                booksRead = defaultData[index],
                totalPages = defaultData[index] * 200, // Assume 200 pages per book
                readingTimeMinutes = defaultData[index] * 300L // Assume 5 hours per book
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}