package com.example.thebook.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.thebook.databinding.FragmentForgetPasswordBinding
import com.example.thebook.utils.Resources

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgetPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentForgetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Handle reset password button
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmailForgotPassword.text.toString().trim()
            if (email.isEmpty()) {
                binding.etEmailForgotPassword.error = getString(com.example.thebook.R.string.hint_email)
                binding.etEmailForgotPassword.requestFocus()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmailForgotPassword.error = getString(com.example.thebook.R.string.invalid_email_format)
                binding.etEmailForgotPassword.requestFocus()
                return@setOnClickListener
            }
            sendPasswordResetEmail(email)
        }

        // Handle login button
        binding.tvBackToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Observe the auth state for password reset
        authViewModel.authState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resources.Loading -> {
                    binding.progressBarForgotPassword.visibility = View.VISIBLE
                    binding.btnResetPassword.isEnabled = false
                }
                is Resources.Success -> {
                    binding.progressBarForgotPassword.visibility = View.GONE
                    binding.btnResetPassword.isEnabled = true
                    Toast.makeText(requireContext(), getString(com.example.thebook.R.string.email_sent_success), Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
                is Resources.Error -> {
                    binding.progressBarForgotPassword.visibility = View.GONE
                    binding.btnResetPassword.isEnabled = true
                    Toast.makeText(requireContext(), getString(com.example.thebook.R.string.reset_password_error) + ": ${resource.exception?.message}", Toast.LENGTH_LONG).show()
                    Log.e("ForgotPassword", "Reset password error: ${resource.exception?.message}")
                }
                else -> {
                }
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        authViewModel.sendPasswordResetEmail(email)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}