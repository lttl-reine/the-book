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
import com.example.thebook.databinding.FragmentRegisterBinding
import com.example.thebook.utils.Resources

class RegisterFragment : Fragment() {
    private val TAG = "RegisterFragment"
    private var _binding : FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel : AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater,container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle register button, create new account
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val name = binding.etName.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && confirmPassword.isNotEmpty()) {
                Log.d(TAG, "Register button clicked, attempting registration for email: $email, name: $name")
                viewModel.register(email, password, confirmPassword, name)
            } else {
                Toast.makeText(context, "Hãy điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle text login, move to login fragment
        binding.tvLogin.setOnClickListener {
            Log.d(TAG, "Login text clicked, navigating to login")
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Handle state of create new account
        viewModel.authState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resources.Loading -> {
                    Log.d(TAG, "Registration in progress")
                    showLoadingState() // Use the unified loading state method
                }
                is Resources.Success -> {
                    Log.d(TAG, "Registration successful: ${binding.etEmail.text.toString()}")
                    hideLoadingState() // Use the unified hiding state method
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }
                is Resources.Error -> {
                    Log.e(TAG, "Registration failed: ${resource.exception.message}")
                    hideLoadingState() // Use the unified hiding state method
                    Toast.makeText(context, "Đăng ký thất bại: ${resource.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Show progress bar in button (Solution 1)
    private fun showLoadingStateInButton() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.text = ""
        binding.btnRegister.icon = null
        binding.btnRegister.isEnabled = false

        // Disable other interactive elements
        binding.etName.isEnabled = false
        binding.etEmail.isEnabled = false
        binding.etPassword.isEnabled = false
        binding.etConfirmPassword.isEnabled = false
        binding.tvLogin.isEnabled = false
        binding.btnGoogleLogin.isEnabled = false
        binding.btnAppleLogin.isEnabled = false
    }

    private fun hideLoadingStateInButton() {
        // Show register button again
        binding.progressBar.visibility = View.GONE
        binding.btnRegister.setText(R.string.register_button)
        binding.btnRegister.setIconResource(R.drawable.ic_add_user_24)
        binding.btnRegister.isEnabled = true

        // Re-enable other interactive elements
        binding.etName.isEnabled = true
        binding.etEmail.isEnabled = true
        binding.etPassword.isEnabled = true
        binding.etConfirmPassword.isEnabled = true
        binding.tvLogin.isEnabled = true
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