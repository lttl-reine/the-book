package com.example.thebook.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.thebook.MainActivity
import com.example.thebook.R
import com.example.thebook.data.model.ReadingStatus
import com.example.thebook.data.model.User
import com.example.thebook.databinding.CustomDialogBinding
import com.example.thebook.databinding.FragmentProfileTabBinding
import com.example.thebook.ui.auth.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileTabFragment : Fragment() {

    private var _binding: FragmentProfileTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileViewModel: ProfileViewModel
    private val authViewModel: AuthViewModel by viewModels()

    companion object {
        private const val TAG = "ProfileTabFragment"

        fun newInstance(): ProfileTabFragment {
            return ProfileTabFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupClickListeners()
        observeData()
        loadUserData()
    }

    private fun setupViewModel() {
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private fun setupClickListeners() {
        binding.apply {
            // Navigation clicks
            headerBar.btnBack.setOnClickListener {
                requireActivity().onBackPressed()
            }

            headerBar.btnMenu.setOnClickListener {
                // Navigate to general settings
                showToast("Settings coming soon")
            }

//            // Profile actions
//            btnEditProfile.setOnClickListener {
//                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
//            }
//
//            // Reading preferences
//            tvEditPreferences.setOnClickListener {
//                showEditPreferencesDialog()
//            }
//
//            // Settings menu
//            llChangePassword.setOnClickListener {
//                startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
//            }
//
//            llNotifications.setOnClickListener {
//                startActivity(Intent(requireContext(), NotificationsActivity::class.java))
//            }
//
//            llTheme.setOnClickListener {
//                startActivity(Intent(requireContext(), ThemeActivity::class.java))
//            }

            // Logout


            // Statistics clicks
            tvBooksCount.setOnClickListener {
                // Navigate to detailed reading history
                showReadingDetails("books")
            }

            tvTotalTime.setOnClickListener {
                showReadingDetails("time")
            }

            tvHistoryLabel.setOnClickListener {
                showReadingDetails("history")
            }
        }

        binding.btnLogout.setOnClickListener {
            Log.d(TAG, "Logout button clicked")
            showLogoutConfirmationDialog()
        }
    }

    private fun observeData() {
        profileViewModel.apply {
            // User data
            currentUser.observe(viewLifecycleOwner) { user ->
                user?.let { updateUserUI(it) }
            }

            // Reading statistics
            readingStats.observe(viewLifecycleOwner) { stats ->
                updateStatisticsUI(stats)
            }

            // Reading preferences
            readingPreferences.observe(viewLifecycleOwner) { preferences ->
                updatePreferencesUI(preferences)
            }

            // Reading habit data
            readingHabitData.observe(viewLifecycleOwner) { habitData ->
                //updateReadingHabitChart(habitData)
            }

            // Loading state
            isLoading.observe(viewLifecycleOwner) { loading ->
                // Show/hide loading indicators if needed
            }

            // Error messages
            errorMessage.observe(viewLifecycleOwner) { error ->
                error?.let {
                    showToast(it)
                    profileViewModel.clearError()
                }
            }
        }
    }

    private fun loadUserData() {
        profileViewModel.loadUserProfile()
        profileViewModel.loadReadingStatistics()
        profileViewModel.loadReadingPreferences()
        profileViewModel.loadReadingHabitData()
    }

    private fun updateUserUI(user: User) {
        binding.apply {
            tvUserName.text = user.name ?: "Unknown User"
            tvUserEmail.text = user.email

            // Load profile image
            if (!user.profileImage.isNullOrEmpty()) {
                Glide.with(this@ProfileTabFragment)
                    .load(user.profileImage)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivProfileImage)
            } else {
                ivProfileImage.setImageResource(R.drawable.ic_profile)
            }
        }
    }

    private fun updateStatisticsUI(stats: ReadingStatistics) {
        binding.apply {
            tvBooksCount.text = stats.totalBooksRead.toString()
            tvTotalTime.text = formatReadingTime(stats.totalReadingTimeMinutes)

            // Update favorite genre
            val favoriteGenre = stats.favoriteGenre ?: "History"
            // You might want to update a TextView that shows the favorite genre
        }
    }

    private fun updatePreferencesUI(preferences: List<String>) {
        binding.apply {
            // Update preference tags visibility based on user preferences
//            tvPrefNonFiction.visibility = if (preferences.contains("Non-fiction")) View.VISIBLE else View.GONE
//            tvPrefSelfHelp.visibility = if (preferences.contains("Self-help")) View.VISIBLE else View.GONE
//            tvPrefHistory.visibility = if (preferences.contains("History")) View.VISIBLE else View.GONE
        }
    }

//    private fun updateReadingHabitChart(habitData: List<MonthlyReadingData>) {
//        val entries = habitData.mapIndexed { index, data ->
//            BarEntry(index.toFloat(), data.booksRead.toFloat())
//        }
//
//        val dataSet = BarDataSet(entries, "Books Read").apply {
//            color = resources.getColor(R.color.primary, null)
//            setDrawValues(false)
//        }
//
//        val barData = BarData(dataSet).apply {
//            barWidth = 0.8f
//        }
//
//        binding.chartReadingHabit.apply {
//            data = barData
//            description.isEnabled = false
//            legend.isEnabled = false
//
//            // Setup X-axis
//            xAxis.apply {
//                position = XAxis.XAxisPosition.BOTTOM
//                setDrawGridLines(false)
//                granularity = 1f
//                valueFormatter = IndexAxisValueFormatter(
//                    habitData.map { it.monthName }
//                )
//            }
//
//            // Setup Y-axis
//            axisLeft.apply {
//                setDrawGridLines(true)
//                gridColor = Color.LTGRAY
//                axisMinimum = 0f
//            }
//
//            axisRight.isEnabled = false
//
//            animateY(1000)
//            invalidate()
//        }
//    }

    private fun showEditPreferencesDialog() {
        val preferences = arrayOf("Fiction", "Non-fiction", "Self-help", "History", "Science", "Biography", "Romance", "Mystery", "Fantasy", "Thriller")
        val selectedPreferences = mutableListOf<String>()

        // Get current preferences
        profileViewModel.readingPreferences.value?.let { currentPrefs ->
            selectedPreferences.addAll(currentPrefs)
        }

        val checkedItems = preferences.map { selectedPreferences.contains(it) }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Reading Preferences")
            .setMultiChoiceItems(preferences, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    selectedPreferences.add(preferences[which])
                } else {
                    selectedPreferences.remove(preferences[which])
                }
            }
            .setPositiveButton("Save") { _, _ ->
                profileViewModel.updateReadingPreferences(selectedPreferences)
                showToast("Preferences updated successfully")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        val dialogBinding = CustomDialogBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // Xử lý nút Yes
        dialogBinding.btnYes.setOnClickListener {
            authViewModel.logout()
            val mainActivityNavController = (activity as? MainActivity)?.navController
            mainActivityNavController?.navigate(R.id.action_global_to_loginFragment)

            dialog.dismiss()
        }

        // Xử lý nút No
        dialogBinding.btnNo.setOnClickListener {
            dialog.dismiss()
        }

        // Hiển thị dialog với nền trong suốt
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun performLogout() {
        profileViewModel.logout()

        // Clear any cached data
        requireActivity().finishAffinity()

        // Navigate to login screen
//        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//        startActivity(intent)
    }

    private fun showReadingDetails(type: String) {
        when (type) {
            "books" -> {
                // Show detailed books read list
                showToast("Books read details coming soon")
            }
            "time" -> {
                // Show reading time breakdown
                showToast("Reading time details coming soon")
            }
            "history" -> {
                // Show reading history
                showToast("Reading history details coming soon")
            }
        }
    }

    private fun formatReadingTime(minutes: Long): String {
        val hours = minutes / 60
        return if (hours > 0) {
            "${hours}h"
        } else {
            "${minutes}m"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Data classes for reading statistics
data class ReadingStatistics(
    val totalBooksRead: Int = 0,
    val totalReadingTimeMinutes: Long = 0L,
    val favoriteGenre: String? = null,
    val booksReadThisMonth: Int = 0,
    val averageRating: Double = 0.0,
    val readingStreak: Int = 0
)

data class MonthlyReadingData(
    val month: Int,
    val monthName: String,
    val booksRead: Int,
    val totalPages: Int,
    val readingTimeMinutes: Long
)