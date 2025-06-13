package com.example.artdecode.presentation.privacy

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.artdecode.R

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var viewModel: PrivacyPolicyViewModel
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_policy)

        setupWindowInsets()
        setupViewModel()
        setupUI()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[PrivacyPolicyViewModel::class.java]
    }

    private fun setupUI() {
        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            viewModel.onBackButtonClicked()
        }
    }

    private fun observeViewModel() {
        viewModel.navigateBack.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                finish()
                viewModel.onNavigationHandled()
            }
        }
    }
}