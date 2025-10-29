// FeaturedBooksAdapter.kt
package com.example.thebook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.databinding.ItemFeaturedBookBinding

class FeaturedBooksAdapter(
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, FeaturedBooksAdapter.FeaturedBookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedBookViewHolder {
        val binding = ItemFeaturedBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeaturedBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedBookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeaturedBookViewHolder(
        private val binding: ItemFeaturedBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            with(binding) {
                // Load book cover
                Glide.with(itemView.context)
                    .load(book.coverImageUrl)
                    .placeholder(R.drawable.book_cover_placeholder)
                    .error(R.drawable.book_cover_placeholder)
                    .transform(RoundedCorners(16))
                    .into(ivFeaturedBookCover)

                // Set book information
                tvFeaturedBookTitle.text = book.title
                tvFeaturedBookAuthor.text = book.author
                tvFeaturedBookDescription.text = book.description


                val gradientColors = arrayOf(
                    R.color.grey_500
                )

                val gradientIndex = adapterPosition % gradientColors.size
                backgroundGradient.setBackgroundResource(gradientColors[gradientIndex])

                // Click listeners
                root.setOnClickListener {
                    onBookClick(book)
                }
            }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.bookId == newItem.bookId
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
}