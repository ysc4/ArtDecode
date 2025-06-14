package com.example.artdecode.presentation.signup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox // Import CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.artdecode.R
import com.example.artdecode.databinding.ActivitySignUpBinding
import com.example.artdecode.presentation.login.LoginActivity
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private val viewModel: SignUpViewModel by viewModels()
    private lateinit var binding: ActivitySignUpBinding // Using ViewBinding, so stick to it

    // Remove these direct variables, use binding.emailInput.text.toString() instead
    // private var email = ""
    // private var username = ""
    // private var password = ""
    // private var confirmPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater) // Initialize binding
        setContentView(binding.root) // Use binding.root

        setupWindowInsets()
        setupUIListeners() // Renamed to use binding
        observeViewModel()
        handleBackPress()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets -> // Use binding.root
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Updated to use ViewBinding
    private fun setupUIListeners() {
        with(binding) {
            emailInput.doAfterTextChanged { /* No need to store in local var if passing directly */ }
            usernameInput.doAfterTextChanged { /* No need to store in local var if passing directly */ }
            passwordInput.doAfterTextChanged { /* No need to store in local var if passing directly */ }
            repeatPassword.doAfterTextChanged { /* No need to store in local var if passing directly */ }

            signUpConfirm.setOnClickListener {
                // Pass all input values and checkbox state to the ViewModel
                viewModel.signUp(
                    emailInput.text.toString().trim(),
                    usernameInput.text.toString().trim(),
                    passwordInput.text.toString(),
                    repeatPassword.text.toString(),
                    checkBox.isChecked // Pass the checked state of the checkbox
                )
            }

            loginLink.setOnClickListener {
                viewModel.navigateToLogin()
            }
        }
    }

    // Updated to use ViewBinding
    private fun observeViewModel() {
        with(binding) { // Use 'with(binding)' to simplify access
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    // Loading state
                    launch {
                        viewModel.isLoading.collect { isLoading ->
                            loadingProgressBar.isVisible = isLoading
                            signUpConfirm.isEnabled = !isLoading
                            signUpConfirm.alpha = if (isLoading) 0.6f else 1.0f
                        }
                    }

                    // Field errors
                    launch {
                        viewModel.emailError.collect { error ->
                            emailInput.error = error
                            if (error != null) emailInput.requestFocus()
                        }
                    }

                    launch {
                        viewModel.usernameError.collect { error ->
                            usernameInput.error = error
                            if (error != null && emailInput.error == null) {
                                usernameInput.requestFocus()
                            }
                        }
                    }

                    launch {
                        viewModel.passwordError.collect { error ->
                            passwordInput.error = error
                            if (error != null && emailInput.error == null && usernameInput.error == null) {
                                passwordInput.requestFocus()
                            }
                        }
                    }

                    launch {
                        viewModel.confirmPasswordError.collect { error ->
                            repeatPassword.error = error
                            if (error != null && emailInput.error == null &&
                                usernameInput.error == null && passwordInput.error == null) {
                                repeatPassword.requestFocus()
                            }
                        }
                    }

                    // Global error
                    launch {
                        viewModel.errorMessage.collect { error ->
                            if (error != null) {
                                errorTextView.text = error
                                errorTextView.isVisible = true
                            } else {
                                errorTextView.isVisible = false
                            }
                        }
                    }

                    // Navigation and Toast messages remain the same
                    launch {
                        viewModel.navigateToLogin.collect { event ->
                            event?.getContentIfNotHandled()?.let {
                                navigateToLogin()
                            }
                        }
                    }

                    launch {
                        viewModel.toastMessage.collect { event ->
                            event?.getContentIfNotHandled()?.let { message ->
                                Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    launch {
                        viewModel.termsError.collect { error ->
                            binding.termsErrorTextView.text = error
                            binding.termsErrorTextView.isVisible = (error != null)
                        }
                    }
                }
            }
        }
    }

    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.navigateToLogin()
            }
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearMessages()
    }
}