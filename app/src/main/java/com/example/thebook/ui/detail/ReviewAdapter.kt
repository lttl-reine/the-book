package com.example.thebook.ui.detail

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.thebook.data.model.Review
import com.example.thebook.data.repository.AuthRepository
import com.example.thebook.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    private val authRepository = AuthRepository()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReviewViewHolder(binding, authRepository)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(
        private val binding: ItemReviewBinding,
        private val authRepository: AuthRepository
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.apply {
                // Set rating stars
                ratingBar.rating = review.rating.toFloat()

                // Set rating text
                tvRating.text = String.format("%.1f", review.rating)

                // Set comment
                tvComment.text = review.comment

                // Set timestamp
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) // Or your desired format
                tvDate.text = dateFormat.format(Date(review.timestamp))

                // Set user name (you might want to fetch this from user profile)

                authRepository.getUserName(review.userId) { userName ->
                    if (userName != null) {
                        tvUserName.text = userName
                    } else {
                        tvUserName.text = "Người dùng ẩn danh (${review.userId.take(4)})"
                        Log.w("ReviewAdapter", "User name not found for ID: ${review.userId}")
                    }
                }
            }
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.userId == newItem.userId &&
                    oldItem.bookId == newItem.bookId &&
                    oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }
}