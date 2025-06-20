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
import com.example.thebook.utils.Resource

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
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                Log.d(TAG, "Login button clicked, attempting login for email: $email")
                viewModel.login(email, password)
            } else {
                Toast.makeText(context, "Hãy điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle sign up text, move to register screen
        binding.tvSignUp.setOnClickListener {
            Log.d(TAG, "Sign up text clicked, navigating to register")
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Handle state of login
        viewModel.authState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    Log.d(TAG, "Login in progress")
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is Resource.Success -> {
                    Log.d(TAG, "Login successful: ${resource.data.email}")
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(context, "Đăng nhập thành công: ${resource.data.email}", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is Resource.Error -> {
                    Log.e(TAG, "Login failed: ${resource.exception.message}")
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(context, "Đăng nhập thất bại: ${resource.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}