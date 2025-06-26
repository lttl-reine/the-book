package com.example.thebook.ui.auth

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
import com.example.thebook.databinding.FragmentLoginBinding
import com.example.thebook.utils.Resources

class LoginFragment : Fragment() {
    private val TAG = "LoginFragment"
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel : AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if user have log in or not
        if (viewModel.isUserLoggedIn()) {
            Log.d(TAG, "User is already logged in, navigating to home")
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }

        // Handle login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                Log.d(TAG, "Login button clicked, attempting login for email: $email")
                viewModel.login(email, password)
            } else {
                Toast.makeText(context, "Hãy điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle sign up text, move to register screen
        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Handle forget password
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        // Handle state of login
        viewModel.authState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resources.Loading -> {
                    Log.d(TAG, "Login in progress")
                    showLoadingState()
                }
                is Resources.Success -> {
                    Log.d(TAG, "Login successful: ${resource.data?.email}")
                    hideLoadingState()
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is Resources.Error -> {
                    Log.e(TAG, "Login failed: ${resource.exception?.message}")
                    hideLoadingState()
                    Toast.makeText(context, "Đăng nhập thất bại: ${resource.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Show progress bar in button
    private fun showLoadingStateInButton() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.text = "" // Ẩn text
        binding.btnLogin.icon = null // Ẩn icon
        binding.btnLogin.isEnabled = false

        // Disable other interactive elements
        binding.etEmail.isEnabled = false
        binding.etPassword.isEnabled = false
        binding.tvSignUp.isEnabled = false
        binding.tvForgotPassword.isEnabled = false
        binding.btnGoogleLogin.isEnabled = false
        binding.btnAppleLogin.isEnabled = false
    }

    private fun hideLoadingStateInButton() {
        // Show login button again
        binding.progressBar.visibility = View.GONE
        binding.btnLogin.setText(R.string.login_button)
        binding.btnLogin.setIconResource(R.drawable.ic_auth_login_24)
        binding.btnLogin.isEnabled = true

        // Re-enable other interactive elements
        binding.etEmail.isEnabled = true
        binding.etPassword.isEnabled = true
        binding.tvSignUp.isEnabled = true
        binding.tvForgotPassword.isEnabled = true
        binding.btnGoogleLogin.isEnabled = true
        binding.btnAppleLogin.isEnabled = true
    }

    private fun showLoadingState() {
        showLoadingStateInButton()
    }

    private fun hideLoadingState() {
        hideLoadingStateInButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}