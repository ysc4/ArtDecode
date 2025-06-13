package com.example.artdecode.presentation.signup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
    private lateinit var binding: ActivitySignUpBinding

    private var email = ""
    private var username = ""
    private var password = ""
    private var confirmPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        setupWindowInsets()
        setupListenersWithFindViewById()
        observeViewModel()
        handleBackPress()
    }

    private fun setupWindowInsets() {
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListenersWithFindViewById() {
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.repeatPassword)
        val signUpButton = findViewById<Button>(R.id.signUpConfirm)
        val loginLinkTextView = findViewById<TextView>(R.id.loginLink)

        emailInput.doAfterTextChanged { email = it.toString().trim() }
        usernameInput.doAfterTextChanged { username = it.toString().trim() }
        passwordInput.doAfterTextChanged { password = it.toString() }
        confirmPasswordInput.doAfterTextChanged { confirmPassword = it.toString() }

        signUpButton.setOnClickListener {
            viewModel.signUp(email, username, password, confirmPassword)
        }

        loginLinkTextView.setOnClickListener {
            viewModel.navigateToLogin()
        }
    }

    // Update observeViewModel to use findViewById
    private fun observeViewModel() {
        val loadingProgressBar = findViewById<ProgressBar>(R.id.loadingProgressBar)
        val signUpButton = findViewById<Button>(R.id.signUpConfirm)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.repeatPassword)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        loadingProgressBar.isVisible = isLoading
                        signUpButton.isEnabled = !isLoading
                        signUpButton.alpha = if (isLoading) 0.6f else 1.0f
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
                        confirmPasswordInput.error = error
                        if (error != null && emailInput.error == null &&
                            usernameInput.error == null && passwordInput.error == null) {
                            confirmPasswordInput.requestFocus()
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