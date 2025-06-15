package com.example.artdecode.presentation.login

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.artdecode.LoginState
import com.example.artdecode.LoginViewModel
import com.example.artdecode.R
import com.example.artdecode.UserInfo
import com.example.artdecode.presentation.signup.SignUpActivity
import com.example.artdecode.databinding.ActivityLoginBinding
import com.example.artdecode.presentation.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    companion object {
        private const val TAG = "LoginActivity"
        // Intent extra keys
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_USER_USERNAME = "extra_user_username"
        const val EXTRA_USER_UID = "extra_user_uid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupUIListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUIListeners() {
        with(binding) {
            // Text change listeners
            email.doAfterTextChanged { text ->
                viewModel.updateEmail(text.toString())
            }
            password.doAfterTextChanged { text ->
                viewModel.updatePassword(text.toString())
            }

            // Button click listeners
            loginBtn.setOnClickListener {
                viewModel.onLoginClicked()
            }

            googleLoginBtn.setOnClickListener {
                viewModel.onGoogleSignInClicked()
            }

            signUpBtn.setOnClickListener {
                viewModel.onSignUpClicked()
            }

            togglePasswordVisibility.setOnClickListener {
                togglePasswordVisibility()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe login state
                launch {
                    viewModel.loginState.collect { state ->
                        handleLoginState(state)
                    }
                }

                // Observe navigation events
                launch {
                    viewModel.navigateToHome.collect { event ->
                        event?.getContentIfNotHandled()?.let { userInfo ->
                            Log.d(TAG, "Navigating to home for user: ${userInfo.uid}")

                            // Show welcome toast with user info
                            showWelcomeToast(userInfo)

                            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                                putExtra(EXTRA_USER_EMAIL, userInfo.email)
                                putExtra(EXTRA_USER_USERNAME, userInfo.username)
                                putExtra(EXTRA_USER_UID, userInfo.uid)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                }

                launch {
                    viewModel.navigateToSignUp.collect { event ->
                        event?.getContentIfNotHandled()?.let {
                            startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
                        }
                    }
                }

                // Observe toast messages
                launch {
                    viewModel.toastMessage.collect { event ->
                        event?.getContentIfNotHandled()?.let { message ->
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // Observe Google Sign-In requests
                launch {
                    viewModel.requestGoogleSignIn.collect { event ->
                        event?.getContentIfNotHandled()?.let { signInRequest ->
                            launchGoogleSignIn(signInRequest)
                        }
                    }
                }
            }
        }
    }

    private fun handleLoginState(state: LoginState) {
        // Clear previous errors
        binding.errorHandlingText.isVisible = false
        binding.email.error = null
        binding.password.error = null

        // Manage loading state
        val isLoading = state is LoginState.Loading
        binding.loginBtn.isEnabled = !isLoading
        binding.googleLoginBtn.isEnabled = !isLoading

        // Show/hide loading indicator if available
        // binding.progressBar.isVisible = isLoading

        when (state) {
            is LoginState.Idle -> {
                // Default state - ensure buttons are enabled
                binding.loginBtn.isEnabled = true
                binding.googleLoginBtn.isEnabled = true
            }
            is LoginState.Loading -> {
                // Loading state handled above
                Log.d(TAG, "Login in progress...")
            }
            is LoginState.Success -> {
                // Navigation handled by observer
                Log.d(TAG, "Login successful")
            }
            is LoginState.Error -> {
                Log.e(TAG, "Login error: ${state.message}")
                showGlobalError(state.message)
            }
            is LoginState.EmailError -> {
                Log.w(TAG, "Email validation error: ${state.message}")
                binding.email.error = state.message
            }
            is LoginState.PasswordError -> {
                Log.w(TAG, "Password validation error: ${state.message}")
                binding.password.error = state.message
            }
            is LoginState.GoogleSignInError -> {
                Log.w(TAG, "Google Sign-In error: ${state.message}")
                showGlobalError(state.message)
            }

            else -> {}
        }
    }

    private fun launchGoogleSignIn(request: GetCredentialRequest) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting Google Sign-In...")

                val credentialManager = CredentialManager.create(this@LoginActivity)
                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity,
                )

                Log.d(TAG, "Google Sign-In credential received")
                // Handle the credential response directly
                viewModel.handleGoogleSignInResult(result)

            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google Sign-In GetCredentialException", e)
                when (e) {
                    is androidx.credentials.exceptions.GetCredentialCancellationException -> {
                        Log.d(TAG, "Google Sign-In was cancelled by user")
                        // Don't show error toast for user cancellation
                    }
                    is androidx.credentials.exceptions.NoCredentialException -> {
                        Log.w(TAG, "No Google credentials available")
                        viewModel.showToast("No Google account found. Please add a Google account to your device.")
                    }
                    else -> {
                        Log.e(TAG, "Google Sign-In credential error: ${e.message}")
                        viewModel.handleGoogleSignInError(e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during Google Sign-In", e)
                viewModel.showToast("Could not start Google Sign-In. Please try again.")
            }
        }
    }

    private fun showGlobalError(message: String) {
        binding.errorHandlingText.text = message
        binding.errorHandlingText.isVisible = true
    }

    private fun showWelcomeToast(userInfo: UserInfo) {
        val welcomeMessage = buildString {
            append("Welcome, ")

            userInfo.username?.let { username ->
                if (username.isNotBlank()) {
                    append(username)
                    userInfo.email?.let { email -> append(" ($email)") }
                } else {
                    userInfo.email?.let { email -> append(email) }
                }
            } ?: userInfo.email?.let { email ->
                append(email)
            } ?: append("User")
        }

        Toast.makeText(this, welcomeMessage, Toast.LENGTH_LONG).show()
        Log.d(TAG, "Showed welcome toast: $welcomeMessage")
    }

    private fun togglePasswordVisibility() {
        binding.password.apply {
            val isPasswordVisible = transformationMethod != PasswordTransformationMethod.getInstance()

            transformationMethod = if (isPasswordVisible) {
                binding.togglePasswordVisibility.setImageResource(R.drawable.eye)
                PasswordTransformationMethod.getInstance()
            } else {
                binding.togglePasswordVisibility.setImageResource(R.drawable.eye_slash)
                HideReturnsTransformationMethod.getInstance()
            }

            // Maintain cursor position
            setSelection(text?.length ?: 0)
        }
    }
}