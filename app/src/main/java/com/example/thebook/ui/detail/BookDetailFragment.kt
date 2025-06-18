package com.example.thebook.ui.detail

import android.os.Bundle
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Category
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.SharedDataRepository
import com.example.thebook.databinding.FragmentBookDetailBinding
import com.example.thebook.util.Resource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class BookDetailFragment : Fragment() {
    private final val TAG  = "BookDetailFragment"

    private var _binding : FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val args: BookDetailFragmentArgs by navArgs()
    private val bookDetailViewModel: BookDetailViewModel by viewModels {
        BookDetailViewModelFactory(BookRepository(), SharedDataRepository(), this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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

        setupUI()
        observeBookDetails()
        bookDetailViewModel.loadBook(args.bookId)
    }

    private fun setupUI() {
        // Handle back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Handle more button
        binding.btnMenu.setOnClickListener {
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
            //
        }

        // Handle add to library button
        binding.btnAddLibrary.setOnClickListener {
            Toast.makeText(context, "Add to library clicked", Toast.LENGTH_LONG).show()
            //
        }

        binding.imgBookCover.setBackgroundResource(R.drawable.book_cover_placeholder) // Đặt placeholder

        // Star icons

    }

    private fun observeBookDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            bookDetailViewModel.book.collectLatest { resource ->
                when (resource) {
                    is Resource.Error -> {
                        //binding.scrollView.visibility = View.GONE
                        Toast.makeText(context, "Error loading book: ${resource.message}", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "observeBookDetails: Error loading book: ${resource.message}")
                        findNavController().navigateUp() 
                    }
                    is Resource.Loading -> {
                        //binding.scrollView.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        //binding.scrollView.visibility = View.VISIBLE
                        resource.data?.let { book ->
                            bookDetailViewModel.categories.collectLatest { categories ->
                                bindBookData(book, categories)
                            }
                        } ?: run {
                            Toast.makeText(context, "Book not found.", Toast.LENGTH_LONG).show()
                            Log.d(TAG, "observeBookDetails: Book not found")
                            findNavController().navigateUp() // Quay lại nếu không tìm thấy sách
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
            tvTitle.text = book.title
            tvBookTitle.text = book.title
            tvAuthor.text = book.author

            // Map category id to display name
            val genreDisplayNames = book.genre.mapNotNull { genreName ->
                category.find { it.name == genreName }?.displayName
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


            // Ratings & Reviews
            tvRating.text = String.format("%.1f/5", book.averageRating)
            tvReviewCount.text = "${book.totalRatings} lượt đánh giá"

            // TODO: Load reviews vào rv_reviews
            // reviewAdapter.submitList(book.reviews)

            // TODO: Load related books vào rv_related_books
            // relatedBooksAdapter.submitList(book.relatedBooks)
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