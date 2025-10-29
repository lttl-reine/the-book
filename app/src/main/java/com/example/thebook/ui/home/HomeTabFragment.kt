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
    private lateinit var featuredBooksAdapter: FeaturedBooksAdapter

    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // Declare handler and runnable for auto-scroll
    private val autoScrollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var autoScrollRunnable: Runnable

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
        setupFeaturedBanner()
        observeViewModelData()
        getCurrentUserType()
    }

    private fun setupFeaturedBanner() {
        featuredBooksAdapter = FeaturedBooksAdapter { book ->
            navigateToBookDetail(book)
        }

        binding.vpFeaturedBooks.adapter = featuredBooksAdapter

        // Setup page transformer để tạo hiệu ứng slide đẹp
        binding.vpFeaturedBooks.setPageTransformer { page, position ->
            val absPosition = Math.abs(position)
            page.apply {
                translationY = absPosition * 50f
                scaleX = 1f - absPosition * 0.1f
                scaleY = 1f - absPosition * 0.1f
                alpha = 1f - absPosition * 0.3f
            }
        }

        // Auto scroll banner
        startAutoScroll()
    }

    private fun startAutoScroll() {
        autoScrollRunnable = object : Runnable {
            override fun run() {
                // Ensure binding is not null before accessing its views
                if (_binding == null) {
                    autoScrollHandler.removeCallbacks(this) // Stop if view is destroyed
                    return
                }
                val currentItem = binding.vpFeaturedBooks.currentItem
                val itemCount = featuredBooksAdapter.itemCount

                if (itemCount > 0) {
                    val nextItem = (currentItem + 1) % itemCount
                    binding.vpFeaturedBooks.setCurrentItem(nextItem, true)
                }

                autoScrollHandler.postDelayed(this, 4000) // 4 giây
            }
        }
        autoScrollHandler.postDelayed(autoScrollRunnable, 4000)
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

        // "Xem tất cả" buttons - truyền category/filter parameters
        binding.tvSeeAllNewest.setOnClickListener {
            navigateToSearchWithFilter("newest")
        }

        binding.tvSeeAllPopular.setOnClickListener {
            navigateToSearchWithFilter("popular")
        }

        binding.tvSeeAllFiction.setOnClickListener {
            navigateToSearchWithFilter("fiction")
        }

        binding.tvSeeAllScience.setOnClickListener {
            navigateToSearchWithFilter("science")
        }

        binding.tvSeeAllHistory.setOnClickListener {
            navigateToSearchWithFilter("history")
        }

        binding.tvSeeAllBooks.setOnClickListener {
            navigateToSearchWithFilter("all")
        }
    }

    // Thêm method mới để navigate với parameters
    private fun navigateToSearchWithFilter(category: String) {
        val bundle = Bundle().apply {
            putString("filter_category", category)
            putString("search_title", getSearchTitle(category))
        }
        findNavController().navigate(R.id.action_homeTabFragment_to_searchFragment, bundle)
    }

    private fun getSearchTitle(category: String): String {
        return when (category) {
            "newest" -> "Sách mới nhất"
            "popular" -> "Sách phổ biến"
            "fiction" -> "Tiểu thuyết"
            "science" -> "Khoa học"
            "history" -> "Lịch sử"
            "all" -> "Tất cả sách"
            else -> "Tìm kiếm"
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

    private fun observeViewModelData() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.newestBooks.collect { resource ->
                    when (resource) {
                        is Resources.Success -> {
                            val books = resource.data
                            if (!books.isNullOrEmpty()) {
                                // Lấy 5 sách đầu tiên cho banner
                                val featuredBooks = books.take(5)
                                featuredBooksAdapter.submitList(featuredBooks)

                                // Ẩn banner nếu không có sách
                                binding.layoutFeaturedBanner.visibility =
                                    if (featuredBooks.isNotEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                        else -> {
                            binding.layoutFeaturedBanner.visibility = View.GONE
                        }
                    }

                    // Xử lý phần còn lại cho RecyclerView bình thường
                    handleNewestBooks(resource)
                }
            }
        }

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
                    // Giới hạn chỉ hiển thị 10 sách đầu tiên
                    val limitedBooks = books.take(10)
                    newestBooksAdapter.submitList(limitedBooks)
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
                    // Giới hạn chỉ hiển thị 10 sách đầu tiên
                    val limitedBooks = books.take(10)
                    popularBooksAdapter.submitList(limitedBooks)
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
                    // Giới hạn chỉ hiển thị 10 sách đầu tiên
                    val limitedBooks = books.take(10)
                    fictionBooksAdapter.submitList(limitedBooks)
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

    // Tương tự cho Science, History và All Books
    private fun handleScienceBooks(resource: Resources<List<Book>>) {
        when (resource) {
            is Resources.Loading -> {
                binding.layoutScienceSection.visibility = View.GONE
            }
            is Resources.Success -> {
                val books = resource.data
                if (!books.isNullOrEmpty()) {
                    binding.layoutScienceSection.visibility = View.VISIBLE
                    val limitedBooks = books.take(10)
                    scienceBooksAdapter.submitList(limitedBooks)
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
                    val limitedBooks = books.take(10)
                    historyBooksAdapter.submitList(limitedBooks)
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
                    val limitedBooks = books.take(10)
                    allBooksAdapter.submitList(limitedBooks)
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