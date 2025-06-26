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
import com.example.thebook.utils.Resources
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.thebook.databinding.FragmentHomeTabBinding
import com.example.thebook.ui.auth.AuthViewModel
import com.example.thebook.utils.setupSystemUI
import kotlinx.coroutines.launch

class HomeTabFragment : Fragment() {
    private var _binding : FragmentHomeTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookAdapter: BookAdapter
    private lateinit var recentlyReadBooksAdapter: BookAdapter
    private val homeViewModel : HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // Enum để quản lý các tab
    private enum class BookFilter {
        ALL, CURRENTLY_READING, COMPLETED
    }

    private var currentFilter = BookFilter.ALL

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

        // Handle search book
        binding.headerBar.ivSearch.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_searchFragment)
        }

        setupSystemUI(
            statusBarColorResId = R.color.white,
            isAppearanceLightStatusBars = true,
            applyInsetsToRoot = true
        )
        setupRecyclerViews()
        setupTabClickListeners()
        observeBooks()
        observeRecentlyReadBooks()

        getCurrentUserType()


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

    private fun setupRecyclerViews() {
        // Setup cho tất cả sách
        bookAdapter = BookAdapter { book ->
            Log.d("HomeTabFragment", "setupRecyclerView: bookId = ${book.bookId}")
            navigateToBookDetail(book)
        }

        binding.rvBooks.apply {
            adapter = bookAdapter
        }

        // Setup cho sách đọc gần đây
        recentlyReadBooksAdapter = BookAdapter { book ->
            Log.d("HomeTabFragment", "setupRecyclerView: recentlyRead bookId = ${book.bookId}")
            navigateToBookDetail(book)
        }

        binding.rvRecentlyReadBooks.apply {
            adapter = recentlyReadBooksAdapter
            setHasFixedSize(true)
            // LinearLayoutManager theo chiều ngang cho sách đọc gần đây
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                context,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun setupTabClickListeners() {
        // Tab "All"
        binding.tvAll.setOnClickListener {
            selectTab(BookFilter.ALL)
        }

        // Tab "Currently Reading"
        binding.tvCurrentlyReading.setOnClickListener {
            selectTab(BookFilter.CURRENTLY_READING)
        }

        // Tab "Completed"
        binding.tvCompleted.setOnClickListener {
            selectTab(BookFilter.COMPLETED)
        }
    }

    private fun selectTab(filter: BookFilter) {
        currentFilter = filter

        // Reset tất cả tab về trạng thái unselected
        binding.tvAll.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_500))
            setBackgroundResource(R.drawable.tab_unselected_background)
        }
        binding.tvCurrentlyReading.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_500))
            setBackgroundResource(R.drawable.tab_unselected_background)
        }
        binding.tvCompleted.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_500))
            setBackgroundResource(R.drawable.tab_unselected_background)
        }

        // Highlight tab được chọn
        when (filter) {
            BookFilter.ALL -> {
                binding.tvAll.apply {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    setBackgroundResource(R.drawable.tab_selected_background)
                }
                observeBooks() // Load all books
            }
            BookFilter.CURRENTLY_READING -> {
                binding.tvCurrentlyReading.apply {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    setBackgroundResource(R.drawable.tab_selected_background)
                }
                observeCurrentlyReadingBooks() // Load currently reading books
            }
            BookFilter.COMPLETED -> {
                binding.tvCompleted.apply {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    setBackgroundResource(R.drawable.tab_selected_background)
                }
                observeCompletedBooks() // Load completed books
            }
        }
    }

    private fun navigateToBookDetail(book: Book) {
        val action = HomeTabFragmentDirections.actionHomeTabFragmentToBookDetailFragment(book.bookId)
        findNavController().navigate(action)
    }

    private fun observeBooks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.books.collect { resource ->
                    when (resource) {
                        is Resources.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is Resources.Success -> {
                            binding.progressBar.visibility = View.GONE
                            bookAdapter.submitList(resource.data)
                        }
                        is Resources.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Error: ${resource.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun observeCurrentlyReadingBooks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.currentlyReadingBooks.collect { resource ->
                    when (resource) {
                        is Resources.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is Resources.Success -> {
                            binding.progressBar.visibility = View.GONE
                            bookAdapter.submitList(resource.data)
                        }
                        is Resources.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Error: ${resource.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun observeCompletedBooks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.finishedBooks.collect { resource ->
                    when (resource) {
                        is Resources.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is Resources.Success -> {
                            binding.progressBar.visibility = View.GONE
                            bookAdapter.submitList(resource.data)
                        }
                        is Resources.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, "Error: ${resource.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun observeRecentlyReadBooks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.recentlyReadBooks.collect { resource ->
                    when (resource) {
                        is Resources.Loading -> {
                            // Ẩn phần recently read khi đang loading
                            binding.tvRecentlyReadLabel.visibility = View.GONE
                            binding.rvRecentlyReadBooks.visibility = View.GONE
                        }
                        is Resources.Success -> {
                            val recentlyReadBooks = resource.data
                            if (!recentlyReadBooks.isNullOrEmpty()) {
                                // Hiển thị phần recently read nếu có dữ liệu
                                binding.tvRecentlyReadLabel.visibility = View.VISIBLE
                                binding.rvRecentlyReadBooks.visibility = View.VISIBLE
                                recentlyReadBooksAdapter.submitList(recentlyReadBooks)
                            } else {
                                // Ẩn phần recently read nếu không có dữ liệu
                                binding.tvRecentlyReadLabel.visibility = View.GONE
                                binding.rvRecentlyReadBooks.visibility = View.GONE
                            }
                        }
                        is Resources.Error -> {
                            // Ẩn phần recently read khi có lỗi
                            binding.tvRecentlyReadLabel.visibility = View.GONE
                            binding.rvRecentlyReadBooks.visibility = View.GONE
                            Log.e("HomeTabFragment", "Error loading recently read books: ${resource.exception?.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data khi quay lại fragment
        homeViewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}