package com.example.artdecode.presentation.login

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
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
import com.example.artdecode.ArtDecode // Import your custom Application class
import com.example.artdecode.R
import com.example.artdecode.presentation.signup.SignUpActivity
import com.example.artdecode.databinding.ActivityLoginBinding
import com.example.artdecode.presentation.main.MainActivity
import com.google.android.material.snackbar.Snackbar // Import Snackbar
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

                // Observe navigation events to Home
                launch {
                    viewModel.navigateToHome.collect { event ->
                        event?.getContentIfNotHandled()?.let { userInfo ->
                            Log.d(TAG, "Navigating to home for user: ${userInfo.uid}")

                            // Show welcome toast with user info
                            showWelcomeToast(userInfo)

                            // --- CRITICAL ADDITION HERE ---
                            // Get the singleton ArtworkRepositoryImpl instance
                            val applicationInstance = application as ArtDecode
                            applicationInstance.artworkRepository.setCurrentUserId(userInfo.uid)
                            Log.d(TAG, "Set UID ${userInfo.uid} on singleton ArtworkRepository from LoginActivity.")
                            // --- END CRITICAL ADDITION ---

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

                // Observe navigation events to Sign Up
                launch {
                    viewModel.navigateToSignUp.collect { event ->
                        event?.getContentIfNotHandled()?.let {
                            startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
                        }
                    }
                }

                // Observe Snackbar messages
                launch {
                    viewModel.snackbarMessage.collect { event ->
                        event.getContentIfNotHandled()?.let { message ->
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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
        // Always clear general error text when state changes, it will be reset if needed.
        binding.errorHandlingText.isVisible = false

        // Manage loading state
        val isLoading = state is LoginState.Loading
        binding.loginBtn.isEnabled = !isLoading
        binding.googleLoginBtn.isEnabled = !isLoading
        // binding.progressBar.isVisible = isLoading // Uncomment if you have a progress bar

        when (state) {
            is LoginState.Idle -> {
                // Clear any lingering input errors when returning to idle
                binding.email.error = null
                binding.password.error = null
            }
            is LoginState.Loading -> {
                // Clear input errors when loading begins
                binding.email.error = null
                binding.password.error = null
                Log.d(TAG, "Login in progress...")
            }
            is LoginState.Success -> {
                // Navigation handled by observer
                Log.d(TAG, "Login successful")
            }
            is LoginState.Error -> {
                Log.e(TAG, "Login error: ${state.message}")
                // Global errors are now handled by Snackbar directly from ViewModel
                // The Snackbar message will be shown via the snackbarMessage SharedFlow.
            }
            is LoginState.InputError -> {
                Log.w(TAG, "Input validation error - Email: ${state.emailError}, Password: ${state.passwordError}")
                binding.email.error = state.emailError // Set error on email input
                binding.password.error = state.passwordError // Set error on password input
            }
            is LoginState.GoogleSignInError -> {
                Log.w(TAG, "Google Sign-In error: ${state.message}")
                // Google Sign-In errors are now handled by Snackbar directly from ViewModel
                // The Snackbar message will be shown via the snackbarMessage SharedFlow.
            }
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
                // ViewModel now handles specific GetCredentialException types and emits Snackbar messages
                viewModel.handleGoogleSignInError(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during Google Sign-In", e)
                viewModel.showSnackbar("Could not start Google Sign-In. Please try again.")
            }
        }
    }

    private fun showWelcomeToast(userInfo: UserInfo) {
        val welcomeMessage = buildString {
            append("Welcome, ")

            userInfo.username?.let { username ->
                if (username.isNotBlank()) {
                    append(username)
                } else {
                    userInfo.email?.let { email -> append(email) }
                }
            } ?: userInfo.email?.let { email ->
                append(email)
            } ?: append("User")
        }

        Snackbar.make(binding.root, welcomeMessage, Snackbar.LENGTH_LONG).show() // Changed to Snackbar
        Log.d(TAG, "Showed welcome message: $welcomeMessage")
    }

    private fun togglePasswordVisibility() {
        binding.password.apply {
            val isPasswordVisible = transformationMethod != PasswordTransformationMethod.getInstance()

            transformationMethod = if (isPasswordVisible) {
                binding.togglePasswordVisibility.setImageResource(R.drawable.eye) // Assuming eye icon for hidden
                PasswordTransformationMethod.getInstance()
            } else {
                binding.togglePasswordVisibility.setImageResource(R.drawable.eye_slash) // Assuming eye_slash for visible
                HideReturnsTransformationMethod.getInstance()
            }

            // Maintain cursor position
            setSelection(text?.length ?: 0)
        }
    }
}