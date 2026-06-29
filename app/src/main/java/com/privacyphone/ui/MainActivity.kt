package com.privacyphone.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.privacyphone.R
import com.privacyphone.service.UsageMonitorService
import com.privacyphone.ui.fragments.HomeFragment
import com.privacyphone.ui.fragments.MediaFragment
import com.privacyphone.ui.fragments.SettingsFragment
import com.privacyphone.ui.fragments.TimeFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start monitor service
        startForegroundService(Intent(this, UsageMonitorService::class.java))

        setupBottomNav()
        if (savedInstanceState == null) showFragment(HomeFragment())
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { showFragment(HomeFragment());    true }
                R.id.nav_time     -> { showFragment(TimeFragment());    true }
                R.id.nav_media    -> { showFragment(MediaFragment());   true }
                R.id.nav_settings -> { showFragment(SettingsFragment()); true }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
