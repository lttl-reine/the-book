package com.example.thebook.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.thebook.MainActivity
import com.example.thebook.R
import com.example.thebook.databinding.CustomDialogBinding
import com.example.thebook.databinding.FragmentProfileTabBinding
import com.example.thebook.ui.auth.AuthViewModel

class ProfileTabFragment : Fragment() {
    private var _binding : FragmentProfileTabBinding? = null
    private val binding get() = _binding!!

    private val authViewModel : AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        val dialogBinding = CustomDialogBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // Xử lý nút Yes
        dialogBinding.btnYes.setOnClickListener {
            authViewModel.logout()
            val mainActivityNavController = (activity as? MainActivity)?.navController
            mainActivityNavController?.navigate(R.id.action_global_to_loginFragment)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}