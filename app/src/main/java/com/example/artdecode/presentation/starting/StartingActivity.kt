package com.example.artdecode.presentation.starting

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.artdecode.R
import com.example.artdecode.presentation.login.LoginActivity

class StartingActivity : AppCompatActivity() {

    private lateinit var viewModel: StartingViewModel
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_starting)

        setupWindowInsets()
        setupViewModel()
        setupUI()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.frameHome)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[StartingViewModel::class.java]
    }

    private fun setupUI() {
        startButton = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            viewModel.onStartButtonClicked()
        }
    }

    private fun observeViewModel() {
        viewModel.navigateToLogin.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                navigateToLogin()
                viewModel.onNavigationHandled()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}