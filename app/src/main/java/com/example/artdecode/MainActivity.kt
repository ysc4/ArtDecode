package com.example.artdecode

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.frameHome)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val homeFragment = Home()
        val settingsFragment = Settings()

        setFragment(homeFragment)

        val bottomNav : BottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navHome -> {
                    setFragment(homeFragment)
                    true
                }
                R.id.navScan -> {
                    // Start the Scan activity
                    val scanIntent = Intent(this, Scan::class.java)
                    startActivity(scanIntent)
                    false // Don't change the selected tab since we're going to another activity
                }
                R.id.navSettings -> {
                    setFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }

        val openFragment = intent.getStringExtra("open_fragment")
        if (openFragment == "settings") {
            val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
            bottomNav.selectedItemId = R.id.navSettings
        }
    }

    private fun setFragment(fragment : Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerView, fragment)
            commit()
        }
}