package com.example.thebook.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.data.model.Library
import com.example.thebook.data.model.ReadingProgress
import com.example.thebook.data.model.ReadingStatus
import com.example.thebook.databinding.ItemLibraryBookBinding
import java.text.SimpleDateFormat
import java.util.*

class LibraryAdapter(
    private val onBookClick: (Book) -> Unit,
    private val onMenuClick: (Library, Book) -> Unit,
    private val onFavoriteClick: (Library, Book) -> Unit
) : ListAdapter<LibraryItem, LibraryAdapter.LibraryViewHolder>(LibraryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val binding = ItemLibraryBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LibraryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LibraryViewHolder(
        private val binding: ItemLibraryBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LibraryItem) {
            val library = item.library
            val book = item.book
            val progress = item.progress

            binding.apply {
                // Book cover
                Glide.with(imgBookCover.context)
                    .load(book.coverImageUrl)
                    .placeholder(R.drawable.book_cover_placeholder)
                    .error(R.drawable.book_cover_placeholder)
                    .into(imgBookCover)

                // Book info
                tvBookTitle.text = book.title
                tvBookAuthor.text = book.author

                // Reading status
                val status = ReadingStatus.valueOf(library.readingStatus)
                tvStatus.text = status.displayName

                // Set status color
                when (status) {
                    ReadingStatus.NOT_STARTED -> {
                        tvStatus.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                    }
                    ReadingStatus.READING -> {
                        tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    }
                    ReadingStatus.FINISHED -> {
                        tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    }
                }

                // Reading progress
                if (progress != null && status == ReadingStatus.READING) {
                    progressReading.visibility = View.VISIBLE
                    tvProgress.visibility = View.VISIBLE

                    val progressPercentage = if (book.pageCount > 0) {
                        (progress.lastReadPage * 100) / book.pageCount
                    } else {
                        0
                    }

                    progressReading.progress = progressPercentage
                    tvProgress.text = "$progressPercentage%"
                } else {
                    progressReading.visibility = View.GONE
                    tvProgress.visibility = View.GONE
                }

                // Favorite icon
                if (library.isFavorite) {
                    imgFavorite.visibility = View.VISIBLE
                    imgFavorite.setImageResource(R.drawable.ic_star_24)
                } else {
                    imgFavorite.visibility = View.GONE
                }

                // Added date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val addedDate = dateFormat.format(Date(library.addedAt))
                tvAddedDate.text = "Thêm ngày $addedDate"

                // Click listeners
                root.setOnClickListener {
                    onBookClick(book)
                }

                btnMenu.setOnClickListener {
                    onMenuClick(library, book)
                }

                imgFavorite.setOnClickListener {
                    onFavoriteClick(library, book)
                }
            }
        }
    }
}

data class LibraryItem(
    val library: Library,
    val book: Book,
    val progress: ReadingProgress? = null
)

class LibraryDiffCallback : DiffUtil.ItemCallback<LibraryItem>() {
    override fun areItemsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
        return oldItem.library.id == newItem.library.id
    }

    override fun areContentsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
        return oldItem.library == newItem.library &&
                oldItem.book == newItem.book &&
                oldItem.progress == newItem.progress
    }
}