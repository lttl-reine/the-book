package com.example.thebook.ui.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.utils.Resource
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.thebook.databinding.FragmentHomeTabBinding
import kotlinx.coroutines.launch

class HomeTabFragment : Fragment() {
    private var _binding : FragmentHomeTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookAdapter: BookAdapter
    private val homeViewModel : HomeViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeTabBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle add book button
        binding.fabAddBook.setOnClickListener{
            findNavController().navigate(R.id.action_homeTabFragment_to_addBookFragment)
        }
        setupSystemUI()
        setupRecyclerView()
        observeBooks()
    }

    private fun setupSystemUI() {
        requireActivity().window.apply {
            // Change status bar background color
            statusBarColor = ContextCompat.getColor(requireContext(), R.color.primary_500)
        }

        // WindowInsetsCompat to handle padding status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            insets.getInsets(WindowInsetsCompat.Type.systemBars())
            insets
        }
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { book ->
            Log.d("HomeTabFragment", "setupRecyclerView: bookId = ${book.bookId}")
            navigateToBookDetail(book)
        }

        binding.rvBooks.apply {
            adapter = bookAdapter
            setHasFixedSize(true)
        }
    }
    private fun navigateToBookDetail(book: Book) {
        val action = HomeTabFragmentDirections.actionHomeTabFragmentToBookDetailFragment(book.bookId)
        findNavController().navigate(action)
    }

    private fun observeBooks() {
        lifecycleScope.launch { // Khởi chạy một coroutine trong lifecycleScope
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Lắng nghe chỉ khi Lifecycle ở trạng thái STARTED trở lên
                homeViewModel.books.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE

                        }
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            bookAdapter.submitList(resource.data)
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Error: ${resource.exception}", Toast.LENGTH_LONG).show()
                        }
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