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
import androidx.activity.R
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        setupCheckboxLinks()
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
                    checkBoxPrivacy.isChecked,
                    checkBoxTerms.isChecked
                )
            }

            loginLink.setOnClickListener {
                viewModel.navigateToLogin()
            }
        }
    }

    private fun setupCheckboxLinks() {
        val privacyText = "I agree to the Privacy Policy"
        val privacyPolicySpannable = SpannableString(privacyText)
        val privacyPolicyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@SignUpActivity, PrivacyPolicyActivity::class.java))
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(this@SignUpActivity, com.example.artdecode.R.color.navy_blue)
            }
        }
        val privacyPolicyStart = privacyText.indexOf("Privacy Policy")
        val privacyPolicyEnd = privacyPolicyStart + "Privacy Policy".length
        privacyPolicySpannable.setSpan(privacyPolicyClickableSpan, privacyPolicyStart, privacyPolicyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.checkBoxPrivacy.apply {
            text = privacyPolicySpannable
            movementMethod = LinkMovementMethod.getInstance()
        }

        val termsText = "I agree to the Terms and Conditions"
        val termsAndConditionsSpannable = SpannableString(termsText)
        val termsAndConditionsClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@SignUpActivity, TermsActivity::class.java))
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(this@SignUpActivity, com.example.artdecode.R.color.navy_blue)
            }
        }
        val termsStart = termsText.indexOf("Terms and Conditions")
        val termsEnd = termsStart + "Terms and Conditions".length
        termsAndConditionsSpannable.setSpan(termsAndConditionsClickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.checkBoxTerms.apply {
            text = termsAndConditionsSpannable
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun observeViewModel() {
        with(binding) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.isLoading.collect { isLoading ->
                            loadingProgressBar.isVisible = isLoading
                            signUpConfirm.isEnabled = !isLoading
                            signUpConfirm.alpha = if (isLoading) 0.6f else 1.0f
                        }
                    }

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