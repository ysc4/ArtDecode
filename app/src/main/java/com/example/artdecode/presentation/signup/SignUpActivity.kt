package com.example.artdecode.presentation.signup

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
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
import com.example.artdecode.databinding.ActivitySignUpBinding
import com.example.artdecode.presentation.login.LoginActivity
import com.example.artdecode.presentation.terms.TermsActivity
import com.example.artdecode.presentation.privacy.PrivacyPolicyActivity
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private val viewModel: SignUpViewModel by viewModels()
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupUIListeners()
        setupCheckboxLinks() // NEW: Set up clickable links within checkboxes
        observeViewModel()
        handleBackPress()
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
            emailInput.doAfterTextChanged { }
            usernameInput.doAfterTextChanged { }
            passwordInput.doAfterTextChanged { }
            repeatPassword.doAfterTextChanged { }

            signUpConfirm.setOnClickListener {
                viewModel.signUp(
                    emailInput.text.toString().trim(),
                    usernameInput.text.toString().trim(),
                    passwordInput.text.toString(),
                    repeatPassword.text.toString(),
                    checkBoxPrivacy.isChecked, // Pass privacy policy checkbox state
                    checkBoxTerms.isChecked // Pass terms and conditions checkbox state
                )
            }

            loginLink.setOnClickListener {
                viewModel.navigateToLogin()
            }
        }
    }

    private fun setupCheckboxLinks() {
        // Privacy Policy CheckBox
        val privacyText = "I agree to the Privacy Policy"
        val privacyPolicySpannable = SpannableString(privacyText)
        val privacyPolicyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Navigate to Privacy Policy Activity
                startActivity(Intent(this@SignUpActivity, PrivacyPolicyActivity::class.java))
            }
            // Optional: Customize link appearance (e.g., remove underline)
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Keep underline for clickable text
                // ds.color = ContextCompat.getColor(this@SignUpActivity, R.color.blue) // Optional: highlight link in blue, define R.color.blue if needed
            }
        }
        val privacyPolicyStart = privacyText.indexOf("Privacy Policy")
        val privacyPolicyEnd = privacyPolicyStart + "Privacy Policy".length
        privacyPolicySpannable.setSpan(privacyPolicyClickableSpan, privacyPolicyStart, privacyPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.checkBoxPrivacy.apply {
            text = privacyPolicySpannable
            movementMethod = LinkMovementMethod.getInstance() // Make links clickable
        }

        // Terms and Conditions CheckBox
        val termsText = "I agree to the Terms and Conditions"
        val termsAndConditionsSpannable = SpannableString(termsText)
        val termsAndConditionsClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Navigate to Terms and Conditions Activity
                startActivity(Intent(this@SignUpActivity, TermsActivity::class.java))
            }
            // Optional: Customize link appearance
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Keep underline for clickable text
                // ds.color = ContextCompat.getColor(this@SignUpActivity, R.color.blue) // Optional: highlight link in blue, define R.color.blue if needed
            }
        }
        val termsStart = termsText.indexOf("Terms and Conditions")
        val termsEnd = termsStart + "Terms and Conditions".length
        termsAndConditionsSpannable.setSpan(termsAndConditionsClickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.checkBoxTerms.apply {
            text = termsAndConditionsSpannable
            movementMethod = LinkMovementMethod.getInstance() // Make links clickable
        }
    }

    private fun observeViewModel() {
        with(binding) {
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

                    // Navigation and Toast messages
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

                    // No longer observing specific terms/privacy error TextViews, handled by toastMessage
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