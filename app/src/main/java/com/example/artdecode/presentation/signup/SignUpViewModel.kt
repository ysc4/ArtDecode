package com.example.artdecode.presentation.signup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artdecode.utils.Event // Ensure you have this Event class
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _confirmPasswordError = MutableStateFlow<String?>(null)
    val confirmPasswordError: StateFlow<String?> = _confirmPasswordError.asStateFlow()

    private val _navigateToLogin = MutableStateFlow<Event<Unit>?>(null)
    val navigateToLogin: StateFlow<Event<Unit>?> = _navigateToLogin.asStateFlow()

    private val _toastMessage = MutableStateFlow<Event<String>?>(null)
    val toastMessage: StateFlow<Event<String>?> = _toastMessage.asStateFlow()

    data class UserData(
        val email: String = "",
        val username: String = "",
        val createdAt: Long = System.currentTimeMillis()
    )

    fun signUp(
        email: String,
        username: String,
        password: String,
        confirmPassword: String,
        agreedToPrivacyPolicy: Boolean,
        agreedToTerms: Boolean
    ) {
        if (!validateInputs(email, username, password, confirmPassword, agreedToPrivacyPolicy, agreedToTerms)) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()

                    try {
                        user.updateProfile(profileUpdates).await()
                    } catch (e: Exception) {
                        _toastMessage.value = Event("Account created, but failed to set username.")
                    }

                    val userData = UserData(
                        email = email,
                        username = username,
                        createdAt = System.currentTimeMillis()
                    )

                    try {
                        database.reference
                            .child("users")
                            .child(user.uid)
                            .setValue(userData)
                            .await()

                        _toastMessage.value = Event("Account created successfully!")
                        _navigateToLogin.value = Event(Unit)

                    } catch (e: Exception) {
                        _errorMessage.value = "Account created but failed to save user data: ${e.message}"
                        _navigateToLogin.value = Event(Unit)
                    }

                } else {
                    _errorMessage.value = "User creation failed"
                }

            } catch (e: Exception) {
                _errorMessage.value = getErrorMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getUserData(userId: String): UserData? {
        return try {
            val snapshot = database.reference
                .child("users")
                .child(userId)
                .get()
                .await()

            snapshot.getValue(UserData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCurrentUserData(): UserData? {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            getUserData(currentUser.uid)
        } else {
            null
        }
    }

    private fun validateInputs(
        email: String,
        username: String,
        password: String,
        confirmPassword: String,
        agreedToPrivacyPolicy: Boolean,
        agreedToTerms: Boolean
    ): Boolean {
        clearErrors()
        var hasError = false

        when {
            email.isBlank() -> { _emailError.value = "Email is required"; hasError = true }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { _emailError.value = "Please enter a valid email"; hasError = true }
        }

        when {
            username.isBlank() -> { _usernameError.value = "Username is required"; hasError = true }
            username.length < 3 -> { _usernameError.value = "Username must be at least 3 characters"; hasError = true }
        }

        when {
            password.isBlank() -> { _passwordError.value = "Password is required"; hasError = true }
            password.length < 6 -> { _passwordError.value = "Password must be at least 6 characters"; hasError = true }
        }

        when {
            confirmPassword.isBlank() -> { _confirmPasswordError.value = "Please confirm your password"; hasError = true }
            password != confirmPassword -> { _confirmPasswordError.value = "Passwords do not match"; hasError = true }
        }

        if (!agreedToPrivacyPolicy) {
            _toastMessage.value = Event("You must agree to the Privacy Policy.")
            hasError = true
        }

        if (!agreedToTerms) {
            _toastMessage.value = Event("You must agree to the Terms and Conditions.")
            hasError = true
        }

        return !hasError
    }

    private fun clearErrors() {
        _emailError.value = null
        _usernameError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
        _errorMessage.value = null
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak"
            is FirebaseAuthInvalidCredentialsException -> "Invalid email format"
            is FirebaseAuthUserCollisionException -> "Account already exists with this email"
            else -> "Failed to create account: ${exception.message}"
        }
    }

    fun navigateToLogin() {
        _navigateToLogin.value = Event(Unit)
    }

    fun clearMessages() {
        _toastMessage.value = null
        _navigateToLogin.value = null
    }
}