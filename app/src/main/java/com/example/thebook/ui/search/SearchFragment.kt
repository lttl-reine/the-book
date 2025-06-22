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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thebook.databinding.FragmentSearchBinding
import com.example.thebook.ui.home.BookAdapter
import com.example.thebook.utils.Resource
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: SearchViewModel by viewModels()
    private val sharedDataViewModel: SharedDataViewModel by viewModels() // // New: ViewModel for shared data
    private lateinit var booksAdapter: BookAdapter
    private lateinit var suggestionsAdapter: SearchSuggestionsAdapter

    // Removed hardcoded genreList as it will be fetched from SharedDataRepository
    // private val genreList = listOf(...)

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

        setupRecyclerViews()
        setupSearchView()
        setupGenreChips() // This will now observe categories
        setupClearFilter()
        observeViewModel()

        // Load initial data
        searchViewModel.searchBooks("")
    }

    private fun setupRecyclerViews() {
        // Books RecyclerView
        booksAdapter = BookAdapter { book ->
            // Navigate to book detail
            // findNavController().navigate(
            //     SearchFragmentDirections.actionSearchToBookDetail(book.bookId)
            // )
        }

        binding.recyclerViewBooks.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
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
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        booksAdapter.submitList(resource.data)
                        binding.textViewResultCount.text = "${resource.data.size} results"
                    }
                    is Resource.Error -> {
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
                    is Resource.Success -> {
                        suggestionsAdapter.submitList(resource.data)
                    }
                    is Resource.Error -> {
                        // Handle error silently for suggestions
                    }
                    is Resource.Loading -> {
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