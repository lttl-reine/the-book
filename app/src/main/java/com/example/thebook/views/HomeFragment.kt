package com.example.thebook.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.thebook.R
import com.example.thebook.databinding.CustomDialogBinding
import com.example.thebook.databinding.FragmentHomeBinding
import com.example.thebook.viewmodel.AuthViewModel

class HomeFragment : Fragment() {
    private var _binding : FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel : AuthViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Gọi fetchCurrentUser để lấy thông tin người dùng
        viewModel.fetchCurrentUser()

        // Quan sát LiveData để cập nhật UI
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvName.text = it.name ?: "Unknown User"
            } ?: run {
                binding.tvName.text = "No User Found"
            }
        }

        // Handle logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        val dialogBinding = CustomDialogBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true) // Cho phép đóng dialog bằng nút Back
            .create()

        // Xử lý nút Yes
        dialogBinding.btnYes.setOnClickListener {
            viewModel.logout()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
            dialog.dismiss()
        }

        // Xử lý nút No
        dialogBinding.btnNo.setOnClickListener {
            dialog.dismiss()
        }

        // Hiển thị dialog với nền trong suốt
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


}