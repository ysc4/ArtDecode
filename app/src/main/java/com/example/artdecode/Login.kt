package com.example.artdecode

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod


class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var errorHandlingText: TextView

    companion object {
        private const val TAG = "Login"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        val usernameInput = findViewById<EditText>(R.id.email)
        val passwordInput = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        val googleSignInButton = findViewById<Button>(R.id.googleLoginBtn)
        errorHandlingText = findViewById<TextView>(R.id.errorHandlingText)

        // Hide error text initially
        errorHandlingText.visibility = View.GONE

        loginButton.setOnClickListener {
            val enteredEmail = usernameInput.text.toString().trim()
            val enteredPassword = passwordInput.text.toString().trim()

            // Hide previous error messages
            errorHandlingText.visibility = View.GONE

            // Validate input
            if (enteredEmail.isEmpty()) {
                showError(getString(R.string.incorrect_email))
                return@setOnClickListener
            }

            if (enteredPassword.isEmpty()) {
                showError(getString(R.string.incorrect_password))
                return@setOnClickListener
            }

            // Sign in with Firebase Auth
            signInWithEmailAndPassword(enteredEmail, enteredPassword)
        }

        googleSignInButton.setOnClickListener {
            errorHandlingText.visibility = View.GONE
            signInWithGoogle()
        }

        // Handle "Don't have an account? Sign Up" click
        findViewById<TextView>(R.id.signUpBtn).setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        val togglePasswordVisibilityButton = findViewById<ImageButton>(R.id.togglePasswordVisibility)

        togglePasswordVisibilityButton.setOnClickListener {
            if (passwordInput.transformationMethod == PasswordTransformationMethod.getInstance()) {
                // Show Password
                passwordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePasswordVisibilityButton.setImageResource(R.drawable.eye_slash) // Change to an 'eye-slash' icon
            } else {
                // Hide Password
                passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePasswordVisibilityButton.setImageResource(R.drawable.eye) // Change back to the 'eye' icon
            }
            // Move cursor to the end of the text
            passwordInput.setSelection(passwordInput.text.length)
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    navigateToHome(user)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            showError(getString(R.string.incorrect_password))
                        }
                        is FirebaseAuthInvalidUserException -> {
                            // "ERROR_USER_NOT_FOUND" - User does not exist.
                            // "ERROR_USER_DISABLED" - User account has been disabled.
                            // "ERROR_INVALID_EMAIL" - Email format is incorrect (though client-side validation is better for this)
                            val errorCode = exception.errorCode
                            if (errorCode == "ERROR_USER_NOT_FOUND") {
                                showError(getString(R.string.user_not_found)) // Or a more specific "User not found"
                            } else if (errorCode == "ERROR_INVALID_EMAIL") {
                                showError(getString(R.string.incorrect_email)) // Or "Invalid email format"
                            } else {
                                showError("User account issue. Please contact support.")
                            }
                        }
                        else -> {
                            Log.e(TAG, "signInWithEmail: unknown error type: ${exception?.javaClass?.name}, message: ${exception?.message}")
                            showError("Authentication failed. Please try again.")
                        }
                    }
                }
            }
    }

    private fun signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-In")

        // Check if default_web_client_id exists
        val clientId = try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "default_web_client_id not found in strings.xml", e)
            Toast.makeText(this, "Configuration error: Missing client ID", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "Using client ID: $clientId")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Requesting credentials...")
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@Login,
                )
                Log.d(TAG, "Credentials received successfully")
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException: ${e.type}", e)
                when (e.type) {
                    "android.credentials.GetCredentialException.TYPE_USER_CANCELED" -> {
                        Toast.makeText(this@Login, "Sign-in was canceled", Toast.LENGTH_SHORT).show()
                    }
                    "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL" -> {
                        Toast.makeText(this@Login, "No Google accounts found", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this@Login, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during Google Sign-In", e)
                Toast.makeText(this@Login, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        Log.d(TAG, "Handling sign-in result")
        when (val credential = result.credential) {
            is CustomCredential -> {
                Log.d(TAG, "Received CustomCredential of type: ${credential.type}")
                if (credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(TAG, "Successfully created GoogleIdTokenCredential")
                        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create GoogleIdTokenCredential", e)
                        Toast.makeText(this, "Failed to process Google credential", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    Toast.makeText(this, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Log.e(TAG, "Unexpected credential class: ${credential::class.java.simpleName}")
                Toast.makeText(this, "Unexpected credential format", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    navigateToHome(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Google Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToHome(user: FirebaseUser?) {
        if (user != null) {
            // User is signed in, navigate to Home activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // User is signed out, stay on login screen
            Log.d(TAG, "User is signed out")
        }
    }

    private fun showError(message: String) {
        errorHandlingText.text = message
        errorHandlingText.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToHome(currentUser)
        }
    }
}