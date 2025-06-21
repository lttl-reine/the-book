package com.example.thebook.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.thebook.data.model.Review
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.databinding.DialogWriteReviewBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth

class WriteReviewDialogFragment(
    private val bookId: String,
    private val onReviewSubmitted: (Review) -> Unit
) : DialogFragment() {

    private var _binding: DialogWriteReviewBinding? = null
    private val binding get() = _binding!!

    private val bookRepository = BookRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWriteReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        setupClickListeners()
    }

    private fun setupDialog() {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set initial rating
        binding.ratingBar.rating = 5.0f
        updateRatingText(5.0f)

        // Rating change listener
        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            updateRatingText(rating)
        }
    }

    private fun updateRatingText(rating: Float) {
        val ratingTexts = mapOf(
            1.0f to "Rất tệ",
            2.0f to "Tệ",
            3.0f to "Bình thường",
            4.0f to "Tốt",
            5.0f to "Rất tốt"
        )
        binding.tvRatingText.text = "${rating.toInt()}/5 - ${ratingTexts[rating] ?: "Tốt"}"
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            submitReview()
        }
    }

    private fun submitReview() {
        val rating = binding.ratingBar.rating.toFloat()
        val comment = binding.etComment.text.toString().trim()

        if (comment.isEmpty()) {
            binding.etComment.error = "Vui lòng nhập nhận xét"
            return
        }

        if (comment.length < 10) {
            binding.etComment.error = "Nhận xét phải có ít nhất 10 ký tự"
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        binding.btnSubmit.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val review = Review(
            bookId = bookId,
            userId = currentUser.uid,
            rating = rating,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )

        // Submit review to repository
        bookRepository.addReview(review) { success, error ->
            activity?.runOnUiThread {
                binding.btnSubmit.isEnabled = true
                binding.progressBar.visibility = View.GONE

                if (success) {
                    Toast.makeText(context, "Đánh giá đã được gửi thành công!", Toast.LENGTH_SHORT).show()
                    onReviewSubmitted(review)
                    dismiss()
                } else {
                    Toast.makeText(context, "Lỗi: ${error ?: "Không thể gửi đánh giá"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "WriteReviewDialog"
    }
}