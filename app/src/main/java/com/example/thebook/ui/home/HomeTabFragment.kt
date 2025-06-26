package com.example.thebook.ui.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.utils.Resources
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.thebook.databinding.FragmentHomeTabBinding
import com.example.thebook.ui.auth.AuthViewModel
import com.example.thebook.utils.setupSystemUI
import kotlinx.coroutines.launch

class HomeTabFragment : Fragment() {
    private var _binding: FragmentHomeTabBinding? = null
    private val binding get() = _binding!!

    // Adapters for different sections
    private lateinit var recentlyReadBooksAdapter: BookAdapter
    private lateinit var newestBooksAdapter: BookAdapter
    private lateinit var popularBooksAdapter: BookAdapter
    private lateinit var fictionBooksAdapter: BookAdapter
    private lateinit var scienceBooksAdapter: BookAdapter
    private lateinit var historyBooksAdapter: BookAdapter
    private lateinit var allBooksAdapter: BookAdapter

    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSystemUI(
            statusBarColorResId = R.color.white,
            isAppearanceLightStatusBars = true,
            applyInsetsToRoot = true
        )

        setupClickListeners()
        setupRecyclerViews()
        setupChipListeners()
        observeViewModelData()
        getCurrentUserType()
    }

    private fun setupClickListeners() {
        // Add book button
        binding.fabAddBook.setOnClickListener {
            findNavController().navigate(R.id.action_homeTabFragment_to_addBookFragment)
        }

        // Search button
        binding.headerBar.ivSearch.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_searchFragment)
        }

        binding.tvSeeAllNewest.setOnClickListener {
            // Navigate to newest books screen
            // findNavController().navigate(R.id.action_to_newestBooksFragment)
        }

        binding.tvSeeAllPopular.setOnClickListener {
            // Navigate to popular books screen
            // findNavController().navigate(R.id.action_to_popularBooksFragment)
        }

        binding.tvSeeAllFiction.setOnClickListener {
            // Navigate to fiction books screen
            // findNavController().navigate(R.id.action_to_fictionBooksFragment)
        }

        binding.tvSeeAllScience.setOnClickListener {
            // Navigate to science books screen
            // findNavController().navigate(R.id.action_to_scienceBooksFragment)
        }

        binding.tvSeeAllHistory.setOnClickListener {
            // Navigate to history books screen
            // findNavController().navigate(R.id.action_to_historyBooksFragment)
        }

        binding.tvSeeAllBooks.setOnClickListener {
            // Navigate to all books screen
            // findNavController().navigate(R.id.action_to_allBooksFragment)
        }
    }

    private fun setupRecyclerViews() {
        // Recently read books
        recentlyReadBooksAdapter = BookAdapter { book ->
            navigateToBookDetail(book)
        }
        binding.rvRecentlyReadBooks.apply {
            adapter = recentlyReadBooksAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        // Newest books
        newestBooksAdapter = BookAdapter { book ->
            navigateToBookDetail(book)
        }
        binding.rvNewestBooks.apply {
            adapter = newestBooksAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        // Popular books
        popularBooksAdapter = BookAdapter { book ->
            navigateToBookDetail(book)
        }
        binding.rvPopularBooks.apply {
            adapter = popularBooksAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        // Fiction books
        fictionBooksAdapter = BookAdapter { book ->
            navigateToBookDetail(book)
        }
        binding.rvFictionBooks.apply {
            adapter = fictionBooksAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        // Science books
        scienceBooksAdapter = BookAdapter { book ->
            navigateToBookDetail(book)
        }
        binding.rvScienceBooks.apply {
            adapter = scienceBooksAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        // History books
        historyBooksAdapter = BookAdapter { book ->
            navigateToBookDetail(book)
        }
        binding.rvHistoryBooks.apply {
            adapter = historyBooksAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        // All books
        allBooksAdapter = BookAdapter { book ->
            navigateToBookDetail(book)
        }
        binding.rvBooks.apply {
            adapter = allBooksAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun setupChipListeners() {
        // Filter chips
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                homeViewModel.setSelectedFilter("all")
                updateChipStates(binding.chipAll)
            }
        }

        binding.chipCurrentlyReading.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                homeViewModel.setSelectedFilter("currently_reading")
                updateChipStates(binding.chipCurrentlyReading)
            }
        }

        binding.chipCompleted.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                homeViewModel.setSelectedFilter("completed")
                updateChipStates(binding.chipCompleted)
            }
        }

        // Sort chips
        binding.chipSortTitle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                homeViewModel.setSelectedSort("title")
            }
        }

        binding.chipSortAuthor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                homeViewModel.setSelectedSort("author")
            }
        }

        binding.chipSortRating.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                homeViewModel.setSelectedSort("rating")
            }
        }
    }

    private fun updateChipStates(selectedChip: com.google.android.material.chip.Chip) {
        // Reset all chips
        binding.chipAll.isChecked = false
        binding.chipCurrentlyReading.isChecked = false
        binding.chipCompleted.isChecked = false

        // Set selected chip
        selectedChip.isChecked = true
    }

    private fun observeViewModelData() {
        // Observe loading state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        // Observe recently read books
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.recentlyReadBooks.collect { resource ->
                    handleRecentlyReadBooks(resource)
                }
            }
        }

        // Observe newest books
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.newestBooks.collect { resource ->
                    handleNewestBooks(resource)
                }
            }
        }

        // Observe popular books
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.popularBooks.collect { resource ->
                    handlePopularBooks(resource)
                }
            }
        }

        // Observe fiction books
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.fictionBooks.collect { resource ->
                    handleFictionBooks(resource)
                }
            }
        }

        // Observe science books
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.scienceBooks.collect { resource ->
                    handleScienceBooks(resource)
                }
            }
        }

        // Observe history books
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.historyBooks.collect { resource ->
                    handleHistoryBooks(resource)
                }
            }
        }

        // Observe all books
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.books.collect { resource ->
                    handleAllBooks(resource)
                }
            }
        }

        // Observe filter changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.selectedFilter.collect { filter ->
                    updateUIBasedOnFilter(filter)
                }
            }
        }
    }

    private fun handleRecentlyReadBooks(resource: Resources<List<Book>>) {
        when (resource) {
            is Resources.Loading -> {
                binding.layoutRecentlyRead.visibility = View.GONE
            }
            is Resources.Success -> {
                val books = resource.data
                if (!books.isNullOrEmpty()) {
                    binding.layoutRecentlyRead.visibility = View.VISIBLE
                    recentlyReadBooksAdapter.submitList(books)
                } else {
                    binding.layoutRecentlyRead.visibility = View.GONE
                }
            }
            is Resources.Error -> {
                binding.layoutRecentlyRead.visibility = View.GONE
                Log.e("HomeTabFragment", "Error loading recently read books: ${resource.exception?.message}")
            }
        }
    }

    private fun handleNewestBooks(resource: Resources<List<Book>>) {
        when (resource) {
            is Resources.Loading -> {
                // Keep section visible but show loading state
            }
            is Resources.Success -> {
                val books = resource.data
                if (!books.isNullOrEmpty()) {
                    newestBooksAdapter.submitList(books)
                }
            }
            is Resources.Error -> {
                Toast.makeText(context, "Error loading newest books: ${resource.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handlePopularBooks(resource: Resources<List<Book>>) {
        when (resource) {
            is Resources.Loading -> {
                // Keep section visible but show loading state
            }
            is Resources.Success -> {
                val books = resource.data
                if (!books.isNullOrEmpty()) {
                    popularBooksAdapter.submitList(books)
                }
            }
            is Resources.Error -> {
                Toast.makeText(context, "Error loading popular books: ${resource.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleFictionBooks(resource: Resources<List<Book>>) {
        when (resource) {
            is Resources.Loading -> {
                binding.layoutFictionSection.visibility = View.GONE
            }
            is Resources.Success -> {
                val books = resource.data
                if (!books.isNullOrEmpty()) {
                    binding.layoutFictionSection.visibility = View.VISIBLE
                    fictionBooksAdapter.submitList(books)
                } else {
                    binding.layoutFictionSection.visibility = View.GONE
                }
            }
            is Resources.Error -> {
                binding.layoutFictionSection.visibility = View.GONE
                Log.e("HomeTabFragment", "Error loading fiction books: ${resource.exception?.message}")
            }
        }
    }

    private fun handleScienceBooks(resource: Resources<List<Book>>) {
        when (resource) {
            is Resources.Loading -> {
                binding.layoutScienceSection.visibility = View.GONE
            }
            is Resources.Success -> {
                val books = resource.data
                if (!books.isNullOrEmpty()) {
                    binding.layoutScienceSection.visibility = View.VISIBLE
                    scienceBooksAdapter.submitList(books)
                } else {
                    binding.layoutScienceSection.visibility = View.GONE
                }
            }
            is Resources.Error -> {
                binding.layoutScienceSection.visibility = View.GONE
                Log.e("HomeTabFragment", "Error loading science books: ${resource.exception?.message}")
            }
        }
    }

    private fun handleHistoryBooks(resource: Resources<List<Book>>) {
        when (resource) {
            is Resources.Loading -> {
                binding.layoutHistorySection.visibility = View.GONE
            }
            is Resources.Success -> {
                val books = resource.data
                if (!books.isNullOrEmpty()) {
                    binding.layoutHistorySection.visibility = View.VISIBLE
                    historyBooksAdapter.submitList(books)
                } else {
                    binding.layoutHistorySection.visibility = View.GONE
                }
            }
            is Resources.Error -> {
                binding.layoutHistorySection.visibility = View.GONE
                Log.e("HomeTabFragment", "Error loading history books: ${resource.exception?.message}")
            }
        }
    }

    private fun handleAllBooks(resource: Resources<List<Book>>) {
        when (resource) {
            is Resources.Loading -> {
                // Keep section visible but show loading state
            }
            is Resources.Success -> {
                val books = resource.data
                if (!books.isNullOrEmpty()) {
                    allBooksAdapter.submitList(books)
                }
            }
            is Resources.Error -> {
                Toast.makeText(context, "Error loading books: ${resource.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIBasedOnFilter(filter: String) {
        when (filter) {
            "all" -> {
                // Show all sections
                binding.layoutRecentlyRead.visibility = View.VISIBLE
                binding.layoutGenreSections.visibility = View.VISIBLE
                binding.layoutAllBooks.visibility = View.VISIBLE
            }
            "currently_reading" -> {
                // Show only currently reading section prominently
                binding.layoutRecentlyRead.visibility = View.GONE
                binding.layoutGenreSections.visibility = View.GONE
                binding.layoutAllBooks.visibility = View.GONE
            }
            "completed" -> {
                // Show completed books section
                binding.layoutRecentlyRead.visibility = View.GONE
                binding.layoutGenreSections.visibility = View.GONE
                binding.layoutAllBooks.visibility = View.VISIBLE
                // You might want to filter the all books to show only completed ones
            }
        }
    }

    private fun getCurrentUserType() {
        authViewModel.fetchCurrentUser()

        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                Log.d("HomeTabFragment", "User UID: ${it.uid}, UserType: ${it.userType}")

                if (it.userType == "admin") {
                    binding.fabAddBook.visibility = View.VISIBLE
                } else {
                    binding.fabAddBook.visibility = View.GONE
                }
            } ?: run {
                Log.d("HomeTabFragment", "Không thể lấy thông tin người dùng. Ẩn nút thêm sách.")
                binding.fabAddBook.visibility = View.GONE
            }
        }
    }

    private fun navigateToBookDetail(book: Book) {
        val action = HomeTabFragmentDirections.actionHomeTabFragmentToBookDetailFragment(book.bookId)
        findNavController().navigate(action)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        homeViewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}