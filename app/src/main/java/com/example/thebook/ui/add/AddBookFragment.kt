package com.example.thebook.ui.add

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Category
import com.example.thebook.data.model.Language
import com.example.thebook.data.repository.AuthRepository
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.SharedDataRepository
import com.example.thebook.databinding.FragmentAddBookBinding
import com.example.thebook.utils.Resources
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddBookFragment : Fragment() {
    private val TAG: String = "AddBookFragment"
    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!

    private val selectedGenres = mutableSetOf<String>()

    private val args: AddBookFragmentArgs by navArgs()
    private var bookIdToEdit: String? = null

    private val addBookViewModel : AddBookViewModel by lazy {
        val sharedDataRepository = SharedDataRepository()
        val bookRepository = BookRepository()
        val authRepository = AuthRepository()
        AddBookViewModelFactory(sharedDataRepository, bookRepository, authRepository).create(
            AddBookViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookIdToEdit = args.bookIdToEdit
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpObservers()
        setupListeners()
        setupImagePreview()

        if (bookIdToEdit != null) {
            // Nếu có bookId, tức là đang ở chế độ chỉnh sửa
            // Gọi ViewModel để lấy thông tin sách và điền vào form
            Log.d(TAG, "Editing existing book: $bookIdToEdit")
            // Có thể đổi text của nút "Thêm sách" thành "Cập nhật sách"
            binding.btnSaveBook.text = "Cập nhật sách"
            addBookViewModel.loadBookToEdit(bookIdToEdit!!)
        } else {
            Log.d(TAG, "Adding new book")
            binding.btnSaveBook.text = "Thêm sách"
        }

    }

    private fun setupImagePreview() {
        binding.etImageUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //
            }

            override fun afterTextChanged(s: Editable?) {
                val imageUrl = s.toString()
                if (imageUrl.isNotBlank() && (imageUrl.startsWith("https://") || imageUrl.startsWith("http://"))) {
                    Glide.with(this@AddBookFragment)
                        .load(imageUrl)
                        .placeholder(R.drawable.book_cover_placeholder)
                        .error(R.drawable.book_cover_error_placeholder)
                        .into(binding.ivCoverPreview)
                } else {
                    binding.ivCoverPreview.setImageResource(R.drawable.book_cover_placeholder)
                }
            }

        })
    }

    @SuppressLint("SetTextI18n")
    private fun setupListeners() {
        // Handle back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Handle save button
        binding.btnSaveBook.setOnClickListener {
            handleAddBookClick()
        }

        // Update number characters of description
        binding.etDescription.addTextChangedListener {
            val currentLength = it?.length ?: 0
            binding.tvCharCount.text = "$currentLength/500"
        }
    }

    private fun setUpObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect categories
                launch {
                    addBookViewModel.categories.collectLatest { categories ->
                        Log.d(TAG, "Received ${categories.size} categories.")
                        populateGenreChips(categories)
                    }
                }

                // Collect languages
                launch {
                    addBookViewModel.languages.collectLatest { languages ->
                        Log.d(TAG, "Received ${languages.size} languages.")
                        val displayNames = languages.map { it.displayName }
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            displayNames
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spinnerLanguage.adapter = adapter
                        binding.spinnerLanguage.tag = languages
                    }
                }

                // Observe save book status
                launch {
                    addBookViewModel.saveBookStatus.collectLatest { status ->
                        when (status) {
                            is SaveBookStatus.Error -> {
                                Log.d(TAG, "Error save book: ${status.message}")
                                binding.progressBar.visibility = View.GONE
                                binding.btnSaveBook.isEnabled = true
                                addBookViewModel.resetSaveBookStatus()
                            }
                            SaveBookStatus.Idle -> {
                                binding.progressBar.visibility = View.GONE
                            }
                            SaveBookStatus.Loading -> {
                                Log.d(TAG, "Saving book ...")
                                binding.progressBar.visibility = View.VISIBLE
                                binding.btnSaveBook.isEnabled = false
                            }
                            is SaveBookStatus.Success -> {
                                Log.d(TAG, status.message)
                                binding.progressBar.visibility = View.GONE
                                binding.btnSaveBook.isEnabled = true
                                clearForm()
                                addBookViewModel.resetSaveBookStatus()

                            }
                        }
                    }
                }

                // Load book info if use update function
                launch {
                    addBookViewModel.bookToEdit.collectLatest { resource ->
                        when (resource) {
                            is Resources.Loading -> {
                                // Hiển thị loading state
                            }
                            is Resources.Success -> {
                                val book = resource.data
                                book?.let { it ->
                                    // Điền dữ liệu sách vào các trường nhập liệu
                                    binding.etTitle.setText(it.title)
                                    binding.etAuthor.setText(it.author)
                                    binding.etDescription.setText(it.description)
                                    binding.etPublishedYear.setText(it.publishedYear.toString())
                                    binding.etPageCount.setText(it.pageCount.toString())
                                    binding.etImageUrl.setText(it.coverImageUrl)
                                    binding.etEpubUrl.setText(it.bookFileUrl)

                                    // Chọn ngôn ngữ - Fixed version
                                    val languages = binding.spinnerLanguage.tag as? List<Language>
                                    languages?.let { languageList ->
                                        val languagePosition = languageList.indexOfFirst { language ->
                                            language.name == it.language
                                        }
                                        if (languagePosition != -1) {
                                            binding.spinnerLanguage.setSelection(languagePosition)
                                        }
                                    }

                                    // Chọn thể loại (genres)
                                    binding.chipGroupGenre.clearCheck()
                                    selectedGenres.clear()
                                    it.genre.forEach { genreId ->
                                        // Find chip by tag (category.id) and check it
                                        for (i in 0 until binding.chipGroupGenre.childCount) {
                                            val chip = binding.chipGroupGenre.getChildAt(i) as? Chip
                                            if (chip?.tag == genreId) {
                                                chip.isChecked = true
                                                selectedGenres.add(genreId)
                                                break
                                            }
                                        }
                                    }

                                    // Load ảnh bìa
                                    if (it.coverImageUrl.isNotEmpty()) {
                                        Glide.with(requireContext())
                                            .load(it.coverImageUrl)
                                            .placeholder(R.drawable.book_cover_placeholder)
                                            .error(R.drawable.book_cover_placeholder)
                                            .into(binding.ivCoverPreview)
                                    } else {
                                        binding.ivCoverPreview.setImageResource(R.drawable.book_cover_placeholder)
                                    }
                                }
                            }
                            is Resources.Error -> {
                                Toast.makeText(context, "Lỗi khi tải sách: ${resource.exception?.message}", Toast.LENGTH_LONG).show()
                                findNavController().popBackStack() // Quay lại nếu có lỗi
                            }
                        }
                    }
                }

            }
        }
    }

    private fun handleAddBookClick() {
        val title = binding.etTitle.text.toString().trim()
        val author = binding.etAuthor.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val publishedYear = binding.etPublishedYear.text.toString().toIntOrNull() ?: 0
        val pageCount = binding.etPageCount.text.toString().toIntOrNull() ?: 0
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val epubUrl = binding.etEpubUrl.text.toString().trim()
        val language = (binding.spinnerLanguage.selectedItem as? Language)?.name ?: "Vietnamese"


        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(context, "Tiêu đề và Tác giả không được để trống", Toast.LENGTH_SHORT).show()
            return
        }

        // Tạo một đối tượng Book tạm thời với dữ liệu từ form
        // bookId sẽ được gán lại nếu là chế độ chỉnh sửa
        val newOrUpdatedBook = Book(
            bookId = bookIdToEdit ?: "", // Nếu là chỉnh sửa, dùng bookId cũ, nếu không thì rỗng (sẽ được Firebase tạo)
            title = title,
            author = author,
            description = description,
            coverImageUrl = imageUrl,
            bookFileUrl = epubUrl,
            publishedYear = publishedYear,
            pageCount = pageCount,
            language = language,
            genre = selectedGenres.toList(), // Chuyển từ Set sang List (nếu đã sửa selectedGenres)
            averageRating = 0.0f, // Sẽ được cập nhật riêng
            totalRatings = 0,    // Sẽ được cập nhật riêng
            uploadDate = System.currentTimeMillis() // Giữ nguyên uploadDate nếu là chỉnh sửa, hoặc dùng ngày hiện tại
        )

        // KIỂM TRA ĐÂY LÀ THÊM MỚI HAY CHỈNH SỬA
        if (bookIdToEdit != null) {
            val currentBookResource = addBookViewModel.bookToEdit.value
            if (currentBookResource is Resources.Success) { // Kiểm tra nếu là trạng thái Success
                currentBookResource.data?.let { originalBook -> // Giờ đây data đã có sẵn
                    val bookForUpdate = newOrUpdatedBook.copy(
                        bookId = originalBook.bookId,
                        averageRating = originalBook.averageRating,
                        totalRatings = originalBook.totalRatings,
                        uploadDate = originalBook.uploadDate,
                        uploaderId = originalBook.uploaderId,
                        free = originalBook.free,
                        price = originalBook.price
                    )
                    addBookViewModel.updateBook(bookForUpdate)

                } ?: Toast.makeText(context, "Thông tin sách gốc trống. Không thể cập nhật.", Toast.LENGTH_SHORT).show()
            } else if (currentBookResource is Resources.Loading) {
                Log.d(TAG, "handleAddBookClick: Loading")
            } else if (currentBookResource is Resources.Error) {
                Log.d(TAG, "handleAddBookClick: error ${currentBookResource.exception?.message}")
            }

            clearForm()
        } else {
            // Chế độ thêm mới
            addBookViewModel.saveBook(newOrUpdatedBook) // Gọi hàm thêm sách mới
        }
    }

    private fun clearForm() {
        binding.etTitle.text?.clear()
        binding.etAuthor.text?.clear()
        binding.etDescription.text?.clear()
        binding.etPublishedYear.text?.clear()
        binding.etPageCount.text?.clear()
        binding.etImageUrl.text?.clear()
        binding.etEpubUrl.text?.clear()
        binding.spinnerLanguage.setSelection(0)
        binding.chipGroupGenre.clearCheck()
        selectedGenres.clear()
        binding.ivCoverPreview.setImageResource(R.drawable.book_cover_placeholder)

    }

    private fun populateGenreChips(categories: List<Category>) {
        binding.chipGroupGenre.removeAllViews()
        Log.d(TAG, "Populating ${categories.size} genre chips.")
        categories.forEach { category ->
            val chip = Chip(requireContext())
            chip.text = category.displayName
            chip.isCheckable = true
            chip.isClickable = true
            chip.tag = category.id


            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedGenres.add(category.id)
                } else {
                    selectedGenres.remove(category.id)
                }
                Log.d(TAG, "Selected Genres: $selectedGenres")
            }
            binding.chipGroupGenre.addView(chip)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}