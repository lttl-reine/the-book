package com.example.thebook.ui.library

import android.util.Log // Import Log
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
    private val onMenuClick: (LibraryItem, View) -> Unit,
    private val onFavoriteClick: (LibraryItem) -> Unit
) : ListAdapter<LibraryItem, LibraryAdapter.LibraryViewHolder>(LibraryDiffCallback()) {

    private val TAG = "LibraryAdapter" // Thêm TAG cho log

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
                tvBookTitle.text = book.title
                tvBookAuthor.text = book.author

                Glide.with(imgBookCover.context)
                    .load(book.coverImageUrl)
                    .placeholder(R.drawable.book_cover_placeholder)
                    .error(R.drawable.book_cover_placeholder)
                    .into(imgBookCover)

                // Log thông tin sách và thư viện
                Log.d(TAG, "Binding book: ${book.title}, Status: ${library.readingStatus}, Page Count: ${book.pageCount}")

                // Hiển thị tiến độ đọc
                if (progress != null && ReadingStatus.valueOf(library.readingStatus) == ReadingStatus.READING) {
                    Log.d(TAG, "Processing progress for book: ${book.title}. Last read page: ${progress.lastReadPage}, Is finished: ${progress.isCompleted}")

                    // Kiểm tra book.pageCount để tránh chia cho 0
                    if (book.pageCount > 0) {
                        val progressPercentage = (progress.lastReadPage * 100) / book.pageCount
                        Log.d(TAG, "Calculated progress for ${book.title}: ${progress.lastReadPage}/${book.pageCount} -> $progressPercentage%")

                        progressReading.progress = progressPercentage
                        tvProgress.text = "$progressPercentage%"
                        layoutProgress.visibility = View.VISIBLE
                    } else {
                        Log.w(TAG, "Book ${book.title} has pageCount 0. Hiding progress.")
                        progressReading.visibility = View.GONE
                        tvProgress.visibility = View.GONE
                    }
                } else {
                    Log.d(TAG, "Hiding progress for book: ${book.title}. Progress is null: ${progress == null}, Status is READING: ${ReadingStatus.valueOf(library.readingStatus) == ReadingStatus.READING}")
                    progressReading.visibility = View.GONE
                    tvProgress.visibility = View.GONE
                }

                // Hiển thị trạng thái yêu thích
                if (library.isFavorite) {
                    imgFavorite.visibility = View.VISIBLE
                    imgFavorite.setImageResource(R.drawable.ic_star_24)
                } else {
                    imgFavorite.visibility = View.GONE
                }
                val currentStatus = ReadingStatus.valueOf(library.readingStatus)
                binding.tvStatus.text = currentStatus.displayName
                // Added date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val addedDate = dateFormat.format(Date(library.addedAt))
                tvAddedDate.text = "Thêm ngày $addedDate"

                // Click listeners
                root.setOnClickListener {
                    onBookClick(book)
                }

                btnMenu.setOnClickListener {
                    onMenuClick(item, btnMenu)
                }

                imgFavorite.setOnClickListener {
                    onFavoriteClick(item)
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
        return oldItem.library.readingStatus == newItem.library.readingStatus &&
                oldItem.library.isFavorite == newItem.library.isFavorite &&
                oldItem.progress?.lastReadPage == newItem.progress?.lastReadPage &&
                oldItem.library.lastReadAt == newItem.library.lastReadAt &&
                oldItem.book == newItem.book
    }
}