package com.example.thebook.ui.library

import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Library
import com.example.thebook.data.model.ReadingStatus
import com.example.thebook.data.repository.LibraryRepository
import com.example.thebook.data.repository.ReadingProgressRepository
import com.example.thebook.databinding.FragmentLibraryTabBinding
import com.example.thebook.utils.Resources
import com.example.thebook.utils.setupSystemUI
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LibraryTabFragment : Fragment() {
    private val TAG = "LibraryTabFragment"

    private var _binding: FragmentLibraryTabBinding? = null
    private val binding get() = _binding!!

    private val libraryViewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory(
            LibraryRepository(),
            ReadingProgressRepository()
        )
    }

    private lateinit var libraryAdapter: LibraryAdapter
    private val auth = FirebaseAuth.getInstance()
    private var currentFilter = ReadingStatus.NOT_STARTED

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSystemUI(
            statusBarColorResId = R.color.white,
            isAppearanceLightStatusBars = true,
            applyInsetsToRoot = true
        )

        setupRecyclerView()
        setupFilterChips()
        observeLibraryData()
        loadLibrary()
    }

    private fun setupRecyclerView() {
        libraryAdapter = LibraryAdapter(
            onBookClick = { book ->
                // Navigate to book detail or reading screen based on progress
                val action = LibraryTabFragmentDirections.actionLibraryTabFragmentToBookDetailFragment(book.bookId)
                findNavController().navigate(action)
            },
            onMenuClick = { library, book ->
                showBookMenu(library, book)
            },
            onFavoriteClick = { library, book ->
                toggleFavorite(library, book)
            }
        )

        binding.rvLibraryBooks.apply {
            adapter = libraryAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupFilterChips() {
        binding.apply {
            chipAll.setOnClickListener {
                filterLibrary(null)
                updateChipSelection(chipAll.id)
            }

            chipNotStarted.setOnClickListener {
                filterLibrary(ReadingStatus.NOT_STARTED)
                updateChipSelection(chipNotStarted.id)
            }

            chipReading.setOnClickListener {
                filterLibrary(ReadingStatus.READING)
                updateChipSelection(chipReading.id)
            }

            chipFinished.setOnClickListener {
                filterLibrary(ReadingStatus.COMPLETED)
                updateChipSelection(chipFinished.id)
            }

            chipFavorites.setOnClickListener {
                loadFavoriteBooks()
                updateChipSelection(chipFavorites.id)
            }
        }

        // Set default selection
        updateChipSelection(binding.chipAll.id)
    }

    private fun updateChipSelection(selectedChipId: Int) {
        binding.apply {
            // Reset all chips
            chipAll.isChecked = false
            chipNotStarted.isChecked = false
            chipReading.isChecked = false
            chipFinished.isChecked = false
            chipFavorites.isChecked = false

            // Set selected chip
            when (selectedChipId) {
                chipAll.id -> chipAll.isChecked = true
                chipNotStarted.id -> chipNotStarted.isChecked = true
                chipReading.id -> chipReading.isChecked = true
                chipFinished.id -> chipFinished.isChecked = true
                chipFavorites.id -> chipFavorites.isChecked = true
            }
        }
    }

    private fun filterLibrary(status: ReadingStatus?) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState("Vui lòng đăng nhập để xem thư viện")
            return
        }

        if (status == null) {
            libraryViewModel.loadUserLibrary(currentUser.uid)
        } else {
            libraryViewModel.loadLibraryByStatus(currentUser.uid, status)
        }
    }

    private fun loadFavoriteBooks() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState("Vui lòng đăng nhập để xem thư viện")
            return
        }

        libraryViewModel.loadFavoriteBooks(currentUser.uid)
    }

    private fun loadLibrary() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState("Vui lòng đăng nhập để xem thư viện")
            return
        }

        libraryViewModel.loadUserLibrary(currentUser.uid)
    }

    private fun observeLibraryData() {
        viewLifecycleOwner.lifecycleScope.launch {
            libraryViewModel.libraryBooks.collect { resource ->
                when (resource) {
                    is Resources.Loading -> {
                        showLoading(true)
                    }
                    is Resources.Success -> {
                        showLoading(false)
                        if (resource.data.isNullOrEmpty()) {
                            showEmptyState("Thư viện của bạn đang trống")
                        } else {
                            showLibraryData(resource.data) // Truyền List<LibraryItem>
                        }
                    }
                    is Resources.Error -> {
                        showLoading(false)
                        Toast.makeText(
                            context,
                            "Lỗi: ${resource.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showLibraryData(libraryItems: List<LibraryItem>) {
        binding.apply {
            rvLibraryBooks.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
            progressBar.visibility = View.GONE
        }
        libraryAdapter.submitList(libraryItems) // Sử dụng LibraryItem trực tiếp
    }

    private fun showEmptyState(message: String) {
        binding.apply {
            rvLibraryBooks.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            tvEmptyMessage.text = message
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showBookMenu(library: Library, book: Book) {
        val popup = PopupMenu(requireContext(), binding.rvLibraryBooks)
        popup.menuInflater.inflate(R.menu.menu_library_book, popup.menu)

        // Update menu items based on current status
        val status = ReadingStatus.valueOf(library.readingStatus)
        popup.menu.findItem(R.id.action_mark_reading).isVisible = status != ReadingStatus.READING
        popup.menu.findItem(R.id.action_mark_finished).isVisible = status != ReadingStatus.COMPLETED
        popup.menu.findItem(R.id.action_mark_not_started).isVisible = status != ReadingStatus.NOT_STARTED

        val favoriteItem = popup.menu.findItem(R.id.action_toggle_favorite)
        favoriteItem.title = if (library.isFavorite) "Bỏ yêu thích" else "Thêm vào yêu thích"

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_mark_reading -> {
                    updateReadingStatus(library, book, ReadingStatus.READING)
                    true
                }
                R.id.action_mark_finished -> {
                    updateReadingStatus(library, book, ReadingStatus.COMPLETED)
                    true
                }
                R.id.action_mark_not_started -> {
                    updateReadingStatus(library, book, ReadingStatus.NOT_STARTED)
                    true
                }
                R.id.action_toggle_favorite -> {
                    toggleFavorite(library, book)
                    true
                }
                R.id.action_remove_from_library -> {
                    removeFromLibrary(library, book)
                    true
                }
                else -> false
            }
        }

        // Add "Continue Reading" option if there's progress
        val continueItem = popup.menu.findItem(R.id.iv_search)
        // This would need to be added to your menu XML

        popup.show()
    }

    private fun navigateToReading(book: Book) {
        val action = LibraryTabFragmentDirections.actionLibraryTabFragmentToBookDetailFragment(book.bookId)
        findNavController().navigate(action)
    }

    private fun updateReadingStatus(library: Library, book: Book, newStatus: ReadingStatus) {
        val currentUser = auth.currentUser ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            libraryViewModel.updateReadingStatus(currentUser.uid, book.bookId, newStatus)

            // Refresh the current filter
            when {
                binding.chipAll.isChecked -> filterLibrary(null)
                binding.chipNotStarted.isChecked -> filterLibrary(ReadingStatus.NOT_STARTED)
                binding.chipReading.isChecked -> filterLibrary(ReadingStatus.READING)
                binding.chipFinished.isChecked -> filterLibrary(ReadingStatus.COMPLETED)
                binding.chipFavorites.isChecked -> loadFavoriteBooks()
            }
        }
    }

    private fun toggleFavorite(library: Library, book: Book) {
        val currentUser = auth.currentUser ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            libraryViewModel.updateFavoriteStatus(currentUser.uid, book.bookId, !library.isFavorite)

            // Refresh the current filter
            when {
                binding.chipAll.isChecked -> filterLibrary(null)
                binding.chipNotStarted.isChecked -> filterLibrary(ReadingStatus.NOT_STARTED)
                binding.chipReading.isChecked -> filterLibrary(ReadingStatus.READING)
                binding.chipFinished.isChecked -> filterLibrary(ReadingStatus.COMPLETED)
                binding.chipFavorites.isChecked -> loadFavoriteBooks()
            }
        }
    }

    private fun removeFromLibrary(library: Library, book: Book) {
        val currentUser = auth.currentUser ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            libraryViewModel.removeBookFromLibrary(currentUser.uid, book.bookId)

            // Refresh the current filter
            when {
                binding.chipAll.isChecked -> filterLibrary(null)
                binding.chipNotStarted.isChecked -> filterLibrary(ReadingStatus.NOT_STARTED)
                binding.chipReading.isChecked -> filterLibrary(ReadingStatus.READING)
                binding.chipFinished.isChecked -> filterLibrary(ReadingStatus.COMPLETED)
                binding.chipFavorites.isChecked -> loadFavoriteBooks()
            }

            Toast.makeText(context, "Đã xóa \"${book.title}\" khỏi thư viện", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment to get latest reading progress
        refreshCurrentView()
    }

    private fun refreshCurrentView() {
        when {
            binding.chipAll.isChecked -> filterLibrary(null)
            binding.chipNotStarted.isChecked -> filterLibrary(ReadingStatus.NOT_STARTED)
            binding.chipReading.isChecked -> filterLibrary(ReadingStatus.READING)
            binding.chipFinished.isChecked -> filterLibrary(ReadingStatus.COMPLETED)
            binding.chipFavorites.isChecked -> loadFavoriteBooks()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}