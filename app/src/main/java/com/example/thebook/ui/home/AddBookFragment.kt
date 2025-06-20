package com.example.thebook.ui.home

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
import com.bumptech.glide.Glide
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Category
import com.example.thebook.data.model.Language
import com.example.thebook.data.repository.AuthRepository
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.data.repository.SharedDataRepository
import com.example.thebook.databinding.FragmentAddBookBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddBookFragment : Fragment() {
    private val TAG: String = "AddBookFragment"
    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!

    private val selectedGenres = mutableListOf<String>()

    private val addBookViewModel : AddBookViewModel by lazy {
        val sharedDataRepository = SharedDataRepository()
        val bookRepository = BookRepository()
        val authRepository = AuthRepository()
        AddBookViewModelFactory(sharedDataRepository, bookRepository, authRepository).create(AddBookViewModel::class.java)
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
            saveBook()
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
            }
        }
    }

    private fun saveBook() {
        val title = binding.etTitle.text.toString()
        val author = binding.etAuthor.text.toString()
        val description = binding.etDescription.text.toString()
        val publishedYear = binding.etPublishedYear.text.toString().toIntOrNull() ?: 0
        val pageCount = binding.etPageCount.text.toString().toIntOrNull() ?: 0
        val coverImageUrl = binding.etImageUrl.text.toString().trim()
        val bookFileUrl = binding.etEpubUrl.text.toString().trim()

        val selectedLanguageIndex = binding.spinnerLanguage.selectedItemPosition
        val languages = binding.spinnerLanguage.tag as? List<Language>
        val selectedLanguageName = if (languages != null && selectedLanguageIndex != -1) {
            languages[selectedLanguageIndex].name
        } else {
            ""
        }

        val genresToSave = selectedGenres.toList()

        if (title.isBlank() || author.isBlank()) {
            Toast.makeText(requireContext(), "Title and Author cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        val newBook = Book(
            title = title,
            author = author,
            genre = genresToSave,
            description = description,
            publishedYear = publishedYear,
            pageCount = pageCount,
            language = selectedLanguageName,
            coverImageUrl = coverImageUrl,
            bookFileUrl = bookFileUrl,
            isFree = true,
            price = 0.0
        )

        addBookViewModel.saveBook(newBook)
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
            chip.tag = category.name


            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedGenres.add(category.name)
                } else {
                    selectedGenres.remove(category.name)
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