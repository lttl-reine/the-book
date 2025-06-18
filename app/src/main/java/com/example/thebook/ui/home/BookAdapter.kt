package com.example.thebook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.databinding.BookItemBinding


class BookAdapter(private val onItemClick: (Book) -> Unit) :
    ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = BookItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book, onItemClick)
    }

    inner class BookViewHolder(private val binding: BookItemBinding):
            RecyclerView.ViewHolder(binding.root) {
                fun bind(book: Book, onItemClick: (Book) -> Unit) {
                    binding.apply {
                        tvBookTitle.text = book.title
                        tvBookAuthor.text = book.title

                        Glide.with(ivBookCover.context)
                            .load(book.coverImageUrl)
                            .placeholder(R.drawable.book_cover_placeholder)
                            .error(R.drawable.book_cover_placeholder)
                            .into(ivBookCover)

                        root.setOnClickListener {
                            onItemClick(book)
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