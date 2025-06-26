package com.example.thebook.ui.detail

import android.os.Bundle
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Category
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.LibraryRepository
import com.example.thebook.data.repository.ReadingProgressRepository
import com.example.thebook.data.repository.SharedDataRepository
import com.example.thebook.databinding.FragmentBookDetailBinding
import com.example.thebook.ui.library.LibraryViewModel
import com.example.thebook.ui.library.LibraryViewModelFactory
import com.example.thebook.utils.Resources
import com.example.thebook.utils.setupSystemUI
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookDetailFragment : Fragment() {
    private val TAG = "BookDetailFragment"

    private var _binding : FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val args: BookDetailFragmentArgs by navArgs()
    private val bookDetailViewModel: BookDetailViewModel by viewModels {
        BookDetailViewModelFactory(BookRepository(), SharedDataRepository(), this)
    }

    private val libraryViewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory(LibraryRepository(), ReadingProgressRepository())
    }

    private var currentBook : Book? = null
    private lateinit var reviewAdapter: ReviewAdapter
    private val bookRepository = BookRepository()
    private val auth = FirebaseAuth.getInstance()
    private var isBookInLibrary = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "bookId = ${args.bookId}")

        setupSystemUI(
            statusBarColorResId = R.color.white,
            isAppearanceLightStatusBars = true,
            applyInsetsToRoot = true
        )

        setupUI()
        setupRecyclerView()
        observeBookDetails()
        observeLibraryActions()
        loadReviews()
        checkBookInLibrary()
        bookDetailViewModel.loadBook(args.bookId)
    }

    private fun setupUI() {
        // Handle back button
        binding.headerBar.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Handle more button
        binding.headerBar.btnMenu.setOnClickListener {
            Toast.makeText(context, "Menu clicked", Toast.LENGTH_LONG).show()
        }

        // Handle click "read more" for description
        binding.tvReadMore.setOnClickListener {
            toggleDescriptionExpansion()
        }

        // Enable scroll description
        binding.tvDescription.movementMethod = ScrollingMovementMethod()

        // Handle start reading button
        binding.btnStartReading.setOnClickListener {
            Toast.makeText(context, "Start Reading clicked", Toast.LENGTH_LONG).show()
            val action = BookDetailFragmentDirections.actionBookDetailFragmentToReaderFragment(
                currentBook!!
            )
            findNavController().navigate(action)
        }

        // Handle add to library button
        binding.btnAddLibrary.setOnClickListener {
            handleLibraryAction()
        }

        // Handle write first review button
        binding.btnWriteFirstReview.setOnClickListener {
            showWriteReviewDialog()
        }
        // Handle write review button
        binding.btnWriteReview.setOnClickListener {
            showWriteReviewDialog()
        }

        // Handle see all reviews
        binding.tvSeeAllReviews.setOnClickListener {
            // Navigate to all reviews screen
            Toast.makeText(context, "See all reviews clicked", Toast.LENGTH_SHORT).show()
        }

        binding.imgBookCover.setBackgroundResource(R.drawable.book_cover_placeholder)
    }

    private fun handleLibraryAction() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm sách vào thư viện", Toast.LENGTH_SHORT).show()
            return
        }

        if (isBookInLibrary) {
            // Remove from library
            libraryViewModel.removeBookFromLibrary(currentUser.uid, args.bookId)
        } else {
            // Add to library
            libraryViewModel.addBookToLibrary(currentUser.uid, args.bookId)
        }
    }

    private fun checkBookInLibrary() {
        val currentUser = auth.currentUser ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            isBookInLibrary = libraryViewModel.isBookInLibrary(currentUser.uid, args.bookId)
            updateLibraryButton()
        }
    }

    private fun updateLibraryButton() {
        if (isBookInLibrary) {
            binding.btnAddLibrary.text = "Xóa khỏi thư viện"
            binding.btnAddLibrary.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_close_24, 0, 0, 0
            )
        } else {
            binding.btnAddLibrary.text = "Thêm vào thư viện"
            binding.btnAddLibrary.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_plus_16, 0, 0, 0
            )
        }
    }

    private fun observeLibraryActions() {
        // Observe add to library result
        viewLifecycleOwner.lifecycleScope.launch {
            libraryViewModel.addToLibraryResult.collectLatest { resource ->
                resource?.let {
                    when (it) {
                        is Resources.Loading -> {
                            binding.btnAddLibrary.isEnabled = false
                        }
                        is Resources.Success -> {
                            binding.btnAddLibrary.isEnabled = true
                            isBookInLibrary = true
                            updateLibraryButton()
                            Toast.makeText(context, "Đã thêm sách vào thư viện", Toast.LENGTH_SHORT).show()
                            libraryViewModel.clearAddToLibraryResult()
                        }
                        is Resources.Error -> {
                            binding.btnAddLibrary.isEnabled = true
                            Toast.makeText(
                                context,
                                "Lỗi: ${it.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            libraryViewModel.clearAddToLibraryResult()
                        }
                    }
                }
            }
        }

        // Observe remove from library result
        viewLifecycleOwner.lifecycleScope.launch {
            libraryViewModel.removeFromLibraryResult.collectLatest { resource ->
                resource?.let {
                    when (it) {
                        is Resources.Loading -> {
                            binding.btnAddLibrary.isEnabled = false
                        }
                        is Resources.Success -> {
                            binding.btnAddLibrary.isEnabled = true
                            isBookInLibrary = false
                            updateLibraryButton()
                            Toast.makeText(context, "Đã xóa sách khỏi thư viện", Toast.LENGTH_SHORT).show()
                            libraryViewModel.clearRemoveFromLibraryResult()
                        }
                        is Resources.Error -> {
                            binding.btnAddLibrary.isEnabled = true
                            Toast.makeText(
                                context,
                                "Lỗi: ${it.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            libraryViewModel.clearRemoveFromLibraryResult()
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter()
        binding.rvReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }
    }

    private fun showWriteReviewDialog() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để viết đánh giá", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user has already reviewed this book
        bookRepository.hasUserReviewed(args.bookId, currentUser.uid) { hasReviewed, error ->
            activity?.runOnUiThread {
                if (error != null) {
                    Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                if (hasReviewed) {
                    Toast.makeText(context, "Bạn đã đánh giá cuốn sách này rồi", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                val dialog = WriteReviewDialogFragment(args.bookId) { newReview ->
                    loadReviews()
                    bookDetailViewModel.loadBook(args.bookId)
                }
                dialog.show(parentFragmentManager, WriteReviewDialogFragment.TAG)
            }
        }
    }

    private fun loadReviews() {
        bookRepository.getReviewsForBook(args.bookId, 3) { reviews, error ->
            activity?.runOnUiThread {
                if (error != null) {
                    Log.e(TAG, "Error loading reviews: $error")
                    return@runOnUiThread
                }

                reviews?.let { reviewList ->
                    reviewAdapter.submitList(reviewList)

                    // Show/hide reviews section based on availability
                    if (reviewList.isEmpty()) {
                        binding.layoutReviewsSection.visibility = View.GONE
                        binding.layoutEmptyReviews.visibility = View.VISIBLE
                    } else {
                        binding.layoutReviewsSection.visibility = View.VISIBLE
                        binding.layoutEmptyReviews.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeBookDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            bookDetailViewModel.book.collectLatest { resource ->
                when (resource) {
                    is Resources.Error -> {
                        Toast.makeText(context, "Error loading book: ${resource.exception}", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "observeBookDetails: Error loading book: ${resource.exception}")
                        findNavController().navigateUp()
                    }
                    is Resources.Loading -> {
                        // You can show loading indicator here
                    }
                    is Resources.Success -> {
                        resource.data?.let { book ->
                            bookDetailViewModel.categories.collectLatest { categories ->
                                bindBookData(book, categories)
                            }
                        } ?: run {
                            Toast.makeText(context, "Book not found.", Toast.LENGTH_LONG).show()
                            Log.d(TAG, "observeBookDetails: Book not found")
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }

    private fun bindBookData(book: Book, category: List<Category>) {
        binding.apply {

            // Book Cover
            Glide.with(imgBookCover.context)
                .load(book.coverImageUrl)
                .placeholder(R.drawable.book_cover_placeholder)
                .error(R.drawable.book_cover_placeholder)
                .into(imgBookCover)

            // Book Info
            headerBar.tvTitle.text = book.title
            tvBookTitle.text = book.title
            tvAuthor.text = book.author

            // Map category id to display name
            val genreDisplayNames = book.genre.mapNotNull { genreName ->
                category.find { it.id == genreName }?.displayName
            }
            tvCategory.text = genreDisplayNames.joinToString(", ")

            tvYear.text = book.publishedYear.toString()
            tvDescription.text = book.description

            // Handle Read More / Read Less for description
            tvDescription.post {
                if (tvDescription.lineCount > 4) {
                    tvDescription.maxLines = 4
                    tvDescription.ellipsize = TextUtils.TruncateAt.END
                    tvReadMore.text = "Đọc thêm..."
                    tvReadMore.visibility = View.VISIBLE
                } else {
                    tvReadMore.visibility = View.GONE
                }
            }

            currentBook = book

            // Ratings & Reviews
            tvRating.text = String.format("%.1f/5", book.averageRating)
            tvReviewCount.text = "${book.totalRatings} lượt đánh giá"
        }
    }

    private fun toggleDescriptionExpansion() {
        binding.tvDescription.apply {
            if (maxLines == 4) { // State "Read more"
                maxLines = Integer.MAX_VALUE
                ellipsize = null
                binding.tvReadMore.text = "Thu gọn"
            } else { // State "Read less"
                maxLines = 4
                ellipsize = TextUtils.TruncateAt.END
                binding.tvReadMore.text = "Đọc thêm..."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}