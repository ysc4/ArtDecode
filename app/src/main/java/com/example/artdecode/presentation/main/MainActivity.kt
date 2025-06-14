package com.example.artdecode.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.artdecode.R
import com.example.artdecode.presentation.home.HomeFragment
import com.example.artdecode.presentation.scan.ScanActivity
import com.example.artdecode.presentation.settings.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp


class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var homeFragment: HomeFragment
    private lateinit var settingsFragment: SettingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.frameHome)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        FirebaseApp.initializeApp(this)
        initializeViews()
        initializeViewModel()
        setupObservers()
        setupBottomNavigation()
        handleIntentData()
    }

    private fun initializeViews() {
        bottomNav = findViewById(R.id.bottomNavigationView)
        homeFragment = HomeFragment()
        settingsFragment = SettingsFragment()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.currentFragmentType.observe(this) { fragmentType ->
            when (fragmentType) {
                "home" -> setFragment(homeFragment)
                "settings" -> setFragment(settingsFragment)
            }
        }

        viewModel.navigateToScan.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                val scanIntent = Intent(this, ScanActivity::class.java)
                startActivity(scanIntent)
                viewModel.onScanNavigationHandled()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navHome -> {
                    viewModel.showHomeFragment()
                    true
                }
                R.id.navScan -> {
                    viewModel.navigateToScanActivity()
                    false // Don't change the selected tab since we're going to another activity
                }
                R.id.navSettings -> {
                    viewModel.showSettingsFragment()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleIntentData() {
        val openFragment = intent.getStringExtra("open_fragment")
        if (openFragment == "settings") {
            bottomNav.selectedItemId = R.id.navSettings
            viewModel.showSettingsFragment()
        }
    }

    private fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerView, fragment)
            commit()
        }
    }
}