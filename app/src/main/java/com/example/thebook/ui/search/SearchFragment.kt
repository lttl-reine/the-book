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
        setupGenreChips() // This will now observe categories
        setupClearFilter()
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

        // Cập nhật title nếu có
        searchTitle?.let {
            // Giả sử bạn có TextView để hiển thị title trong header
            // binding.headerBar.tvTitle.text = it
        }

        // Tự động search theo category
        filterCategory?.let { category ->
            when (category) {
                "newest" -> {
                    searchViewModel.searchBooksWithFilter("", "newest")
                    // Ẩn search suggestions và genre chips vì đây là filter cụ thể
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
            // Nếu không có filter, load initial data như bình thường
            searchViewModel.searchBooks("")
        }
    }

    private fun selectGenreChip(genreName: String) {
        // Sẽ được gọi sau khi chips đã được tạo
        lifecycleScope.launch {
            sharedDataViewModel.categories.collect { categories ->
                val targetCategory = categories.find { it.name.equals(genreName, ignoreCase = true) }
                targetCategory?.let { category ->
                    // Tìm và select chip tương ứng
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
        // Books RecyclerView
        booksAdapter = BookAdapter { book ->
             //Navigate to book detail
             findNavController().navigate(
                 SearchFragmentDirections.actionSearchFragmentToBookDetailFragment(book.bookId)
             )
        }

        binding.recyclerViewBooks.apply {
            adapter = booksAdapter
        }

        // Suggestions RecyclerView
        suggestionsAdapter = SearchSuggestionsAdapter { suggestion ->
            binding.searchView.setQuery(suggestion, true)
            binding.recyclerViewSuggestions.visibility = View.GONE
        }

        binding.recyclerViewSuggestions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = suggestionsAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchViewModel.searchBooks(it)
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
            sharedDataViewModel.categories.collect { categories -> // // Observe categories from SharedDataViewModel
                binding.chipGroupGenres.removeAllViews() // Clear existing chips before adding new ones
                categories.forEach { category -> //
                    val chip = Chip(requireContext()) //
                    chip.text = category.displayName // // Use displayName for UI
                    chip.isCheckable = true //
                    chip.setOnCheckedChangeListener { _, isChecked -> //
                        if (isChecked) { //
                            // Clear other chips
                            for (i in 0 until binding.chipGroupGenres.childCount) { //
                                val otherChip = binding.chipGroupGenres.getChildAt(i) as Chip //
                                if (otherChip != chip) { //
                                    otherChip.isChecked = false //
                                }
                            }
                            searchViewModel.filterByGenre(category.name) // // Use 'name' for filtering logic
                        } else { //
                            searchViewModel.clearGenreFilter() //
                        }
                    }
                    binding.chipGroupGenres.addView(chip) //
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

    private fun observeViewModel() {
        // Observe search results
        lifecycleScope.launch {
            searchViewModel.searchResults.collect { resource ->
                when (resource) {
                    is Resources.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Resources.Success -> {
                        binding.progressBar.visibility = View.GONE
                        booksAdapter.submitList(resource.data)
                        binding.textViewResultCount.text = "${resource.data.size} results"
                    }
                    is Resources.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Search failed: ${resource.exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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