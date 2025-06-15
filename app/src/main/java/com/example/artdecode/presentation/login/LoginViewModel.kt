package com.example.artdecode

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
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
    data class EmailError(val message: String) : LoginState()
    data class PasswordError(val message: String) : LoginState()
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

    private val _toastMessage = MutableStateFlow<Event<String>?>(null)
    val toastMessage: StateFlow<Event<String>?> = _toastMessage.asStateFlow()

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
        // No need to checkCurrentUser() anymore if we're always signing out
    }

    // This method is no longer needed as we're always signing out on init
    // private fun checkCurrentUser() {
    //     val currentUser = auth.currentUser
    //     if (currentUser != null) {
    //         fetchUserInfoAndNavigate(currentUser)
    //     }
    // }

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
            }
        })
    }

    fun updateEmail(newEmail: String) {
        email = newEmail.trim()
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun showToast(message: String) {
        _toastMessage.value = Event(message)
    }

    fun onLoginClicked() {
        if (email.isEmpty()) {
            _loginState.value = LoginState.EmailError(getApplication<Application>().getString(R.string.incorrect_email))
            return
        }
        if (password.isEmpty()) {
            _loginState.value = LoginState.PasswordError(getApplication<Application>().getString(R.string.incorrect_password))
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    Log.d(TAG, "signInWithEmail:success")
                    _loginState.value = LoginState.Success(user)
                    fetchUserInfoAndNavigate(user)
                } else {
                    _loginState.value = LoginState.Error("Login successful but user data is null.")
                }
            } catch (exception: Exception) {
                Log.w(TAG, "signInWithEmail:failure", exception)
                val app = getApplication<Application>()
                when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        _loginState.value = LoginState.PasswordError(app.getString(R.string.incorrect_password))
                    }
                    is FirebaseAuthInvalidUserException -> {
                        val errorCode = exception.errorCode
                        when (errorCode) {
                            "ERROR_USER_NOT_FOUND" -> {
                                _loginState.value = LoginState.EmailError(app.getString(R.string.user_not_found))
                            }
                            "ERROR_INVALID_EMAIL" -> {
                                _loginState.value = LoginState.EmailError(app.getString(R.string.incorrect_email))
                            }
                            else -> {
                                _loginState.value = LoginState.Error("User account issue. Please contact support.")
                            }
                        }
                    }
                    else -> {
                        Log.e(TAG, "signInWithEmail: unknown error type: ${exception.javaClass.name}, message: ${exception.message}")
                        _loginState.value = LoginState.Error("Authentication failed. Please try again.")
                    }
                }
            }
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
            _loginState.value = LoginState.GoogleSignInError("Configuration error: Missing client ID")
            _toastMessage.value = Event("Configuration error: Missing client ID")
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
            _loginState.value = LoginState.GoogleSignInError("Google Sign-In failed or was canceled.")
            _toastMessage.value = Event("Google Sign-In failed or was canceled.")
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
                        _loginState.value = LoginState.GoogleSignInError("Failed to process Google credential")
                        _toastMessage.value = Event("Failed to process Google credential")
                    }
                } else {
                    Log.e(TAG, "Unexpected Google credential type: ${credential.type}")
                    _loginState.value = LoginState.GoogleSignInError("Unexpected Google credential type")
                    _toastMessage.value = Event("Unexpected Google credential type")
                }
            }
            else -> {
                Log.e(TAG, "Unexpected Google credential class: ${credential::class.java.simpleName}")
                _loginState.value = LoginState.GoogleSignInError("Unexpected Google credential format")
                _toastMessage.value = Event("Unexpected Google credential format")
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
        _toastMessage.value = Event(message)
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
                    _loginState.value = LoginState.Error("Google login successful but user data is null.")
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Google signInWithCredential:failure", exception)
                _loginState.value = LoginState.GoogleSignInError("Google Authentication Failed.")
                _toastMessage.value = Event("Google Authentication Failed.")
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
            }
    }

    fun onSignUpClicked() {
        _navigateToSignUp.value = Event(Unit)
    }
}