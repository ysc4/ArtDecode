package com.example.artdecode

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signUpButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var errorTextView: TextView

    companion object {
        private const val TAG = "SignUp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews()

        // Set click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.editTextTextEmailAddress)
        usernameInput = findViewById(R.id.editTextText)
        passwordInput = findViewById(R.id.editTextTextPassword)
        confirmPasswordInput = findViewById(R.id.editTextTextPassword2)
        signUpButton = findViewById(R.id.button3)

        // Add ProgressBar and error TextView to your layout
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        errorTextView = findViewById(R.id.errorTextView)

        // Initially hide loading and error views
        loadingProgressBar.visibility = View.GONE
        errorTextView.visibility = View.GONE
    }

    private fun setupClickListeners() {
        signUpButton.setOnClickListener {
            attemptSignUp()
        }

        // Handle "Already have an account? Login" click
        findViewById<TextView>(R.id.loginLinkTextView).setOnClickListener {
            navigateToLogin()
        }
    }

    private fun attemptSignUp() {
        // Hide previous errors
        hideError()

        // Get input values
        val email = emailInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        // Validate inputs
        if (!validateInputs(email, username, password, confirmPassword)) {
            return
        }

        // Show loading
        showLoading(true)

        // Create user account with Firebase
        createUserAccount(email, password, username)
    }

    private fun validateInputs(email: String, username: String, password: String, confirmPassword: String): Boolean {
        when {
            email.isEmpty() -> {
                showError("Email is required")
                emailInput.requestFocus()
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError("Please enter a valid email address")
                emailInput.requestFocus()
                return false
            }
            username.isEmpty() -> {
                showError("Username is required")
                usernameInput.requestFocus()
                return false
            }
            username.length < 3 -> {
                showError("Username must be at least 3 characters long")
                usernameInput.requestFocus()
                return false
            }
            password.isEmpty() -> {
                showError("Password is required")
                passwordInput.requestFocus()
                return false
            }
            password.length < 6 -> {
                showError("Password must be at least 6 characters long")
                passwordInput.requestFocus()
                return false
            }
            confirmPassword.isEmpty() -> {
                showError("Please confirm your password")
                confirmPasswordInput.requestFocus()
                return false
            }
            password != confirmPassword -> {
                showError("Passwords do not match")
                confirmPasswordInput.requestFocus()
                return false
            }
        }
        return true
    }

    private fun createUserAccount(email: String, password: String, username: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser

                    // Update user profile with username
                    user?.let {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()

                        it.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    Log.d(TAG, "User profile updated.")
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                                    // Navigate to Login or Home activity
                                    navigateToLogin()
                                } else {
                                    Log.w(TAG, "Failed to update user profile", profileTask.exception)
                                    // Still consider signup successful, just profile update failed
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    navigateToLogin()
                                }
                            }
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    handleSignUpError(task.exception)
                }
            }
    }

    private fun handleSignUpError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please choose a stronger password."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email format. Please enter a valid email address."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please use a different email or try logging in."
            else -> "Failed to create account: ${exception?.message ?: "Unknown error"}"
        }
        showError(errorMessage)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingProgressBar.visibility = View.VISIBLE
            signUpButton.isEnabled = false
            signUpButton.alpha = 0.6f
        } else {
            loadingProgressBar.visibility = View.GONE
            signUpButton.isEnabled = true
            signUpButton.alpha = 1.0f
        }
    }

    private fun showError(message: String) {
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorTextView.visibility = View.GONE
    }

    private fun navigateToLogin() {
        // Navigate back to Login activity
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Navigate back to Login activity when back button is pressed
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }
}