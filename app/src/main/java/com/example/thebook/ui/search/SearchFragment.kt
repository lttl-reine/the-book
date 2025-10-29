package com.example.thebook.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thebook.R
import com.example.thebook.databinding.FragmentSearchBinding
import com.example.thebook.ui.home.BookAdapter
import com.example.thebook.utils.Resources
import com.example.thebook.utils.setupSystemUI
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: SearchViewModel by viewModels()
    private val sharedDataViewModel: SharedDataViewModel by viewModels()
    private lateinit var booksAdapter: BookAdapter
    private lateinit var suggestionsAdapter: SearchSuggestionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSystemUI(
            statusBarColorResId = R.color.white,
            isAppearanceLightStatusBars = true,
            applyInsetsToRoot = true
        )
        setupRecyclerViews()
        setupSearchView()
        setupGenreChips()
        setupClearFilter()
        setupPullToRefresh()
        observeViewModel()

        // Load initial data
        handleIncomingParameters()

        // Handle back button
        binding.headerBar.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun handleIncomingParameters() {
        val filterCategory = arguments?.getString("filter_category")
        val searchTitle = arguments?.getString("search_title")

        // Update title if available
        searchTitle?.let {
            // Assuming you have TextView to display title in header
            // binding.headerBar.tvTitle.text = it
        }

        // Auto search by category
        filterCategory?.let { category ->
            when (category) {
                "newest" -> {
                    searchViewModel.searchBooksWithFilter("", "newest")
                    // Hide search suggestions and genre chips for specific filters
                    binding.layoutFilters.visibility = View.GONE
                }
                "popular" -> {
                    searchViewModel.searchBooksWithFilter("", "popular")
                    binding.layoutFilters.visibility = View.GONE
                }
                "fiction" -> {
                    searchViewModel.filterByGenre("fiction")
                    // Auto-select fiction chip
                    selectGenreChip("fiction")
                }
                "science" -> {
                    searchViewModel.filterByGenre("science")
                    selectGenreChip("science")
                }
                "history" -> {
                    searchViewModel.filterByGenre("history")
                    selectGenreChip("history")
                }
                "all" -> {
                    searchViewModel.searchBooks("")
                }
            }
        } ?: run {
            // If no filter, load initial data as normal
            searchViewModel.searchBooks("")
        }
    }

    private fun selectGenreChip(genreName: String) {
        // Will be called after chips have been created
        lifecycleScope.launch {
            sharedDataViewModel.categories.collect { categories ->
                val targetCategory = categories.find { it.name.equals(genreName, ignoreCase = true) }
                targetCategory?.let { category ->
                    // Find and select corresponding chip
                    for (i in 0 until binding.chipGroupGenres.childCount) {
                        val chip = binding.chipGroupGenres.getChildAt(i) as Chip
                        if (chip.text.toString() == category.displayName) {
                            chip.isChecked = true
                            break
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Books RecyclerView with pagination
        booksAdapter = BookAdapter { book ->
            // Navigate to book detail
            findNavController().navigate(
                SearchFragmentDirections.actionSearchFragmentToBookDetailFragment(book.bookId)
            )
        }

        val layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerViewBooks.apply {
            this.adapter = booksAdapter
            this.layoutManager = layoutManager

            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // Load more when reaching near end
                    if (!searchViewModel.uiState.value.isLoadingMore &&
                        searchViewModel.uiState.value.hasMore &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4) {
                        searchViewModel.loadNextPage()
                    }
                }
            })
        }

        // Suggestions RecyclerView
        suggestionsAdapter = SearchSuggestionsAdapter { suggestion ->
            binding.searchView.setQuery(suggestion, true)
            binding.recyclerViewSuggestions.visibility = View.GONE
        }

        binding.recyclerViewSuggestions.apply {
            adapter = suggestionsAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchViewModel.searchBooks(it, resetPagination = true)
                    binding.recyclerViewSuggestions.visibility = View.GONE
                    binding.searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (it.length >= 2) {
                        searchViewModel.getSuggestions(it)
                        binding.recyclerViewSuggestions.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewSuggestions.visibility = View.GONE
                    }
                }
                return true
            }
        })

        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.recyclerViewSuggestions.visibility = View.GONE
            }
        }
    }

    private fun setupGenreChips() {
        lifecycleScope.launch {
            sharedDataViewModel.categories.collect { categories ->
                binding.chipGroupGenres.removeAllViews()
                categories.forEach { category ->
                    val chip = Chip(requireContext())
                    chip.text = category.displayName
                    chip.isCheckable = true
                    chip.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            // Clear other chips
                            for (i in 0 until binding.chipGroupGenres.childCount) {
                                val otherChip = binding.chipGroupGenres.getChildAt(i) as Chip
                                if (otherChip != chip) {
                                    otherChip.isChecked = false
                                }
                            }
                            searchViewModel.filterByGenre(category.name, resetPagination = true)
                        } else {
                            searchViewModel.clearGenreFilter()
                        }
                    }
                    binding.chipGroupGenres.addView(chip)
                }
            }
        }
    }

    private fun setupClearFilter() {
        binding.textViewClearFilter.setOnClickListener {
            // Clear all genre chips
            for (i in 0 until binding.chipGroupGenres.childCount) {
                val chip = binding.chipGroupGenres.getChildAt(i) as Chip
                chip.isChecked = false
            }
            searchViewModel.clearGenreFilter()
        }
    }

    private fun setupPullToRefresh() {
        // If you have SwipeRefreshLayout in your layout
        // binding.swipeRefreshLayout?.setOnRefreshListener {
        //     searchViewModel.retryLastSearch()
        // }
    }

    private fun observeViewModel() {
        // Observe UI state
        lifecycleScope.launch {
            searchViewModel.uiState.collect { uiState ->
                // Handle loading state
                binding.progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE

                // Handle loading more state
                if (uiState.isLoadingMore) {
                    // Show loading more indicator (you might want to add this to your layout)
                    // binding.progressBarLoadMore?.visibility = View.VISIBLE
                } else {
                    // binding.progressBarLoadMore?.visibility = View.GONE
                }

                // Update books list
                booksAdapter.submitList(uiState.books)

                // Update result count
                val resultText = if (uiState.totalCount > 0) {
                    "${uiState.books.size} of ${uiState.totalCount} results"
                } else {
                    "${uiState.books.size} results"
                }
                binding.textViewResultCount.text = resultText

                // Handle error state
                uiState.error?.let { error ->
                    Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
                }

                // Handle empty state
                if (!uiState.isLoading && uiState.books.isEmpty() && uiState.error == null) {
                    // Show empty state
                    binding.textViewResultCount.text = "No results found"
                }

                // Hide pull to refresh loading
                // binding.swipeRefreshLayout?.isRefreshing = false
            }
        }

        // Observe suggestions
        lifecycleScope.launch {
            searchViewModel.suggestions.collect { resource ->
                when (resource) {
                    is Resources.Success -> {
                        suggestionsAdapter.submitList(resource.data)
                    }
                    is Resources.Error -> {
                        // Handle error silently for suggestions
                    }
                    is Resources.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}