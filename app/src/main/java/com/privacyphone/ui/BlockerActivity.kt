package com.privacyphone.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.privacyphone.R
import com.privacyphone.data.prefs.AppPreferences
import com.privacyphone.util.UsageStatsHelper

class BlockerActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocker)

        prefs = AppPreferences(this)

        val packageName = intent.getStringExtra("package_name") ?: ""
        val limitMin = intent.getIntExtra("limit_minutes", 60)
        val usedMin = intent.getLongExtra("used_minutes", 0)

        // Get app name
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val overUsed = usedMin - limitMin
        findViewById<TextView>(R.id.tv_blocked_app).text =
            "$appName đã dùng ${UsageStatsHelper.formatMinutes(usedMin)}\n" +
            "Giới hạn: ${UsageStatsHelper.formatMinutes(limitMin.toLong())}"

        // Reset time display
        val resetMin = 60 - (usedMin % 60)
        findViewById<TextView>(R.id.tv_time_remaining).text =
            "Mở lại lúc: ${resetMin} phút nữa"

        // Go home button
        findViewById<Button>(R.id.btn_go_home).setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        }

        // Override with PIN
        findViewById<Button>(R.id.btn_override).setOnClickListener {
            showPinDialog()
        }
    }

    private fun showPinDialog() {
        var enteredPin = ""
        val pinDisplay = TextView(this).apply {
            text = "• • • •"
            textSize = 24f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        AlertDialog.Builder(this, R.style.DarkDialog)
            .setTitle("Nhập PIN để mở khóa")
            .setView(pinDisplay)
            .setPositiveButton("Xác nhận") { _, _ ->
                if (enteredPin == prefs.pin) {
                    finish() // Let user use the app
                } else {
                    showError()
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showError() {
        AlertDialog.Builder(this, R.style.DarkDialog)
            .setMessage("PIN không đúng!")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onBackPressed() {
        // Go home instead of back to blocked app
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
