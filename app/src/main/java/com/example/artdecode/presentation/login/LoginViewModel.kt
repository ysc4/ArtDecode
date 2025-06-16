package com.example.artdecode.presentation.login

import android.app.Application
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.artdecode.R
import com.example.artdecode.utils.Event
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// Data class to hold user information
data class UserInfo(
    val uid: String,
    val email: String?,
    val username: String?
)

// Represents the different states of the login process
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: FirebaseUser) : LoginState()
    data class Error(val message: String) : LoginState()
    // Specific error types for direct input feedback
    data class InputError(val emailError: String? = null, val passwordError: String? = null) : LoginState()
    data class GoogleSignInError(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(application)

    // StateFlow for login state
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // StateFlow for navigation events - now using UserInfo instead of FirebaseUser
    private val _navigateToHome = MutableStateFlow<Event<UserInfo>?>(null)
    val navigateToHome: StateFlow<Event<UserInfo>?> = _navigateToHome.asStateFlow()

    private val _navigateToSignUp = MutableStateFlow<Event<Unit>?>(null)
    val navigateToSignUp: StateFlow<Event<Unit>?> = _navigateToSignUp.asStateFlow()

    // NEW: SharedFlow for Snackbar messages
    private val _snackbarMessage = MutableSharedFlow<Event<String>>()
    val snackbarMessage: SharedFlow<Event<String>> = _snackbarMessage

    private val _requestGoogleSignIn = MutableStateFlow<Event<GetCredentialRequest>?>(null)
    val requestGoogleSignIn: StateFlow<Event<GetCredentialRequest>?> = _requestGoogleSignIn.asStateFlow()

    // User input
    private var email: String = ""
    private var password: String = ""

    init {
        // Always sign out the current user when the ViewModel is initialized
        // This ensures the login screen always starts from a logged-out state.
        auth.signOut()
        Log.d(TAG, "FirebaseAuth signed out at ViewModel init.")
    }

    private fun fetchUserInfoAndNavigate(firebaseUser: FirebaseUser) {
        val userRef = database.getReference("users").child(firebaseUser.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = firebaseUser.email ?: snapshot.child("email").getValue(String::class.java)

                val userInfo = UserInfo(
                    uid = firebaseUser.uid,
                    email = email,
                    username = username
                )

                Log.d(TAG, "User info fetched - Email: $email, Username: $username")
                _navigateToHome.value = Event(userInfo)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch user info", error.toException())
                // Still navigate with basic info from FirebaseUser
                val userInfo = UserInfo(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    username = firebaseUser.displayName // Fallback to display name if available
                )
                _navigateToHome.value = Event(userInfo)
                showSnackbar("Failed to retrieve full user information.")
            }
        })
    }

    fun updateEmail(newEmail: String) {
        email = newEmail.trim()
        // Clear previous email-related errors when user starts typing
        if (_loginState.value is LoginState.InputError) {
            val currentErrorState = _loginState.value as LoginState.InputError
            if (currentErrorState.emailError != null) {
                _loginState.value = currentErrorState.copy(emailError = null)
            }
        }
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
        // Clear previous password-related errors when user starts typing
        if (_loginState.value is LoginState.InputError) {
            val currentErrorState = _loginState.value as LoginState.InputError
            if (currentErrorState.passwordError != null) {
                _loginState.value = currentErrorState.copy(passwordError = null)
            }
        }
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(Event(message))
        }
    }

    fun onLoginClicked() {
        val app = getApplication<Application>()
        var emailError: String? = null
        var passwordError: String? = null

        if (email.isEmpty()) {
            emailError = app.getString(R.string.email_required) // More specific message
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = app.getString(R.string.incorrect_email_format)
        }

        if (password.isEmpty()) {
            passwordError = app.getString(R.string.password_required)
        } else if (password.length < 6) { // Example: Add a minimum password length check
            passwordError = app.getString(R.string.password_too_short)
        }

        if (emailError != null || passwordError != null) {
            _loginState.value = LoginState.InputError(emailError, passwordError)
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    Log.d(TAG, "signInWithEmail:success for user ${user.uid}")
                    _loginState.value = LoginState.Success(user)
                    fetchUserInfoAndNavigate(user)
                } else {
                    Log.e(TAG, "Login successful but user data is null for email: $email")
                    val errorMessage = "Login successful but user data is null."
                    _loginState.value = LoginState.Error(errorMessage)
                    showSnackbar(errorMessage)
                }
            } catch (exception: Exception) {
                Log.w(TAG, "signInWithEmail:failure for email $email", exception)
                handleFirebaseAuthException(exception)
            }
        }
    }

    private fun handleFirebaseAuthException(exception: Exception) {
        val app = getApplication<Application>()
        Log.e(TAG, "handleFirebaseAuthException: EXCEPTION TYPE: ${exception.javaClass.name}, MESSAGE: ${exception.message}")

        var emailError: String? = null
        var passwordError: String? = null
        var generalErrorMessage: String? = null

        when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                val errorCode = exception.errorCode
                Log.e(TAG, "FirebaseAuthInvalidCredentialsException. SPECIFIC ERROR CODE: $errorCode")
                when (errorCode) {
                    "ERROR_INVALID_EMAIL" -> {
                        emailError = app.getString(R.string.incorrect_email_format)
                    }
                    "ERROR_WRONG_PASSWORD" -> {
                        passwordError = app.getString(R.string.incorrect_password)
                    }
                    else -> {
                        generalErrorMessage = app.getString(R.string.authentication_failed_generic)
                    }
                }
            }
            is FirebaseAuthInvalidUserException -> {
                val errorCode = exception.errorCode
                Log.e(TAG, "FirebaseAuthInvalidUserException. SPECIFIC ERROR CODE: $errorCode")
                when (errorCode) {
                    "ERROR_USER_NOT_FOUND" -> {
                        emailError = app.getString(R.string.user_not_found)
                    }
                    "ERROR_USER_DISABLED" -> {
                        generalErrorMessage = app.getString(R.string.user_disabled)
                    }
                    else -> {
                        generalErrorMessage = app.getString(R.string.authentication_failed_generic)
                    }
                }
            }
            else -> {
                Log.e(TAG, "Unknown Firebase Exception Type. Message: ${exception.message}")
                generalErrorMessage = app.getString(R.string.authentication_failed_generic) + ". Please try again."
            }
        }

        if (emailError != null || passwordError != null) {
            _loginState.value = LoginState.InputError(emailError, passwordError)
            val message = emailError ?: (passwordError ?: app.getString(R.string.please_correct_input_errors))
            showSnackbar(message)
        } else if (generalErrorMessage != null) {
            _loginState.value = LoginState.Error(generalErrorMessage)
            showSnackbar(generalErrorMessage)
        } else {
            val fallbackMessage = app.getString(R.string.authentication_failed_generic) + ". Please try again."
            _loginState.value = LoginState.Error(fallbackMessage)
            showSnackbar(fallbackMessage)
        }
    }

    fun onGoogleSignInClicked() {
        _loginState.value = LoginState.Loading
        Log.d(TAG, "Starting Google Sign-In")

        val app = getApplication<Application>()
        val clientId = try {
            app.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "default_web_client_id not found in strings.xml", e)
            val errorMessage = "Configuration error: Missing client ID"
            _loginState.value = LoginState.GoogleSignInError(errorMessage)
            showSnackbar(errorMessage)
            return
        }

        Log.d(TAG, "Using client ID: $clientId")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        _requestGoogleSignIn.value = Event(request)
    }

    fun handleGoogleSignInResult(result: GetCredentialResponse?) {
        if (result == null) {
            val message = "Google Sign-In failed or was canceled."
            _loginState.value = LoginState.GoogleSignInError(message)
            showSnackbar(message)
            return
        }

        Log.d(TAG, "Handling Google sign-in result")
        when (val credential = result.credential) {
            is CustomCredential -> {
                Log.d(TAG, "Received CustomCredential of type: ${credential.type}")
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(TAG, "Successfully created GoogleIdTokenCredential")
                        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create GoogleIdTokenCredential", e)
                        val errorMessage = "Failed to process Google credential"
                        _loginState.value = LoginState.GoogleSignInError(errorMessage)
                        showSnackbar(errorMessage)
                    }
                } else {
                    Log.e(TAG, "Unexpected Google credential type: ${credential.type}")
                    val errorMessage = "Unexpected Google credential type"
                    _loginState.value = LoginState.GoogleSignInError(errorMessage)
                    showSnackbar(errorMessage)
                }
            }
            else -> {
                Log.e(TAG, "Unexpected Google credential class: ${credential::class.java.simpleName}")
                val errorMessage = "Unexpected Google credential format"
                _loginState.value = LoginState.GoogleSignInError(errorMessage)
                showSnackbar(errorMessage)
            }
        }
    }

    fun handleGoogleSignInError(e: GetCredentialException) {
        Log.e(TAG, "GetCredentialException: ${e.type}", e)
        val message = when (e.type) {
            "androidx.credentials.GetCredentialException.TYPE_USER_CANCELED",
            "android.credentials.GetCredentialException.TYPE_USER_CANCELED" -> "Sign-in was canceled"
            "androidx.credentials.GetCredentialException.TYPE_NO_CREDENTIAL",
            "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL" -> "No Google accounts found"
            else -> "Google Sign-In failed: ${e.message}"
        }
        _loginState.value = LoginState.GoogleSignInError(message)
        showSnackbar(message)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user

                if (user != null) {
                    Log.d(TAG, "Google signInWithCredential:success")
                    _loginState.value = LoginState.Success(user)

                    // For Google Sign-In, also save/update user data in database if it's a new user
                    if (result.additionalUserInfo?.isNewUser == true) {
                        saveUserToDatabase(user)
                    }

                    fetchUserInfoAndNavigate(user)
                } else {
                    val errorMessage = "Google login successful but user data is null."
                    _loginState.value = LoginState.Error(errorMessage)
                    showSnackbar(errorMessage)
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Google signInWithCredential:failure", exception)
                val errorMessage = "Google Authentication Failed."
                _loginState.value = LoginState.GoogleSignInError(errorMessage)
                showSnackbar(errorMessage)
            }
        }
    }

    private fun saveUserToDatabase(user: FirebaseUser) {
        val userRef = database.getReference("users").child(user.uid)
        val userData = mapOf(
            "email" to user.email,
            "username" to (user.displayName ?: ""),
            "provider" to "google"
        )

        userRef.setValue(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User data saved to database")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save user data", exception)
                showSnackbar("Failed to save user data: ${exception.localizedMessage}")
            }
    }

    fun onSignUpClicked() {
        _navigateToSignUp.value = Event(Unit)
    }
}