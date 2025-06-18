package com.example.thebook.ui.home

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.thebook.R
import com.example.thebook.data.model.Book
import com.example.thebook.util.Resource
import androidx.lifecycle.lifecycleScope
import com.example.thebook.databinding.FragmentHomeTabBinding

class HomeTabFragment : Fragment() {
    private var _binding : FragmentHomeTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookAdapter: BookAdapter
    private val homeViewModel : HomeViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeTabBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle add book button
        binding.fabAddBook.setOnClickListener{
            findNavController().navigate(R.id.action_homeTabFragment_to_addBookFragment)
        }
        setupRecyclerView()
        observeBooks()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { book ->
            Log.d("HomeTabFragment", "setupRecyclerView: bookId = ${book.bookId}")
            navigateToBookDetail(book)
        }

        binding.rvBooks.apply {
            adapter = bookAdapter
            setHasFixedSize(true)
        }
    }
    private fun navigateToBookDetail(book: Book) {
        val action = HomeTabFragmentDirections.actionHomeTabFragmentToBookDetailFragment(book.bookId)
        findNavController().navigate(action)
    }

    private fun observeBooks() {
        lifecycleScope.launchWhenStarted {
            homeViewModel.books.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
//                        binding.progressBarLoading.visibility = View.VISIBLE

                    }
                    is Resource.Success -> {
//                        binding.progressBarLoading.visibility = View.GONE
                        bookAdapter.submitList(resource.data)
                    }
                    is Resource.Error -> {
//                        binding.progressBarLoading.visibility = View.GONE
                        // Hiển thị thông báo lỗi
                         Toast.makeText(context, "Error: ${resource.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    private fun showLogoutConfirmationDialog() {
//        val dialogBinding = CustomDialogBinding.inflate(LayoutInflater.from(requireContext()))
//        val dialog = AlertDialog.Builder(requireContext())
//            .setView(dialogBinding.root)
//            .setCancelable(true) // Cho phép đóng dialog bằng nút Back
//            .create()
//
//        // Xử lý nút Yes
//        dialogBinding.btnYes.setOnClickListener {
//            viewModel.logout()
//            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
//            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
//            dialog.dismiss()
//        }
//
//        // Xử lý nút No
//        dialogBinding.btnNo.setOnClickListener {
//            dialog.dismiss()
//        }
//
//        // Hiển thị dialog với nền trong suốt
//        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
//        dialog.show()
//    }


}