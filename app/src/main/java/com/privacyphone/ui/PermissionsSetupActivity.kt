package com.privacyphone.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.privacyphone.R
import com.privacyphone.util.PermissionsHelper

class PermissionsSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions_setup)
        renderPermissions()
    }

    override fun onResume() {
        super.onResume()
        renderPermissions()
    }

    private fun renderPermissions() {
        val container = findViewById<LinearLayout>(R.id.permissions_container)
        container.removeAllViews()

        val items = listOf(
            Triple(
                PermissionsHelper.hasUsageStatsPermission(this),
                PermissionsHelper.PermissionItem.USAGE_STATS
            ) { PermissionsHelper.openUsageAccessSettings(this) },
            Triple(
                PermissionsHelper.hasAccessibilityPermission(this),
                PermissionsHelper.PermissionItem.ACCESSIBILITY
            ) { PermissionsHelper.openAccessibilitySettings(this) },
            Triple(
                PermissionsHelper.isDeviceAdminActive(this),
                PermissionsHelper.PermissionItem.DEVICE_ADMIN
            ) { PermissionsHelper.requestDeviceAdmin(this) }
        )

        var allGranted = true
        items.forEach { (granted, perm, action) ->
            if (!granted) allGranted = false
            addPermissionRow(container, granted, perm.title, perm.desc, action)
        }

        // Show continue button if all granted
        val continueBtn = findViewById<Button>(R.id.btn_continue_main)
        continueBtn.visibility = if (allGranted) View.VISIBLE else View.GONE
        continueBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Update skip button
        val skipBtn = findViewById<Button>(R.id.btn_skip)
        skipBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun addPermissionRow(
        container: LinearLayout,
        granted: Boolean,
        title: String,
        desc: String,
        onClick: () -> Unit
    ) {
        val inflater = layoutInflater
        val row = inflater.inflate(R.layout.item_permission_row, container, false)

        row.findViewById<TextView>(R.id.tv_perm_title).text = title
        row.findViewById<TextView>(R.id.tv_perm_desc).text = desc

        val statusIcon = row.findViewById<TextView>(R.id.tv_status_icon)
        val grantBtn = row.findViewById<Button>(R.id.btn_grant)

        if (granted) {
            statusIcon.text = "✅"
            grantBtn.visibility = View.GONE
        } else {
            statusIcon.text = "⚠️"
            grantBtn.visibility = View.VISIBLE
            grantBtn.setOnClickListener { onClick() }
        }

        container.addView(row)
    }
}
