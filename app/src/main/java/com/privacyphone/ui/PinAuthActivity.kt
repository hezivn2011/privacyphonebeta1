package com.privacyphone.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.privacyphone.R
import com.privacyphone.data.prefs.AppPreferences

class PinAuthActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private var enteredPin = ""
    private var failCount = 0
    private val pinDots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_auth)

        prefs = AppPreferences(this)

        setupPinDots()
        setupNumpad()

        // Show question option if security question is set
        if (prefs.securityQuestion.isNotEmpty()) {
            findViewById<TextView>(R.id.tv_use_question)?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_use_question)?.setOnClickListener {
                showSecurityQuestionDialog()
            }
        }
    }

    private fun setupPinDots() {
        val container = findViewById<LinearLayout>(R.id.pin_dots_container)
        container.removeAllViews()
        pinDots.clear()
        repeat(8) {
            val dot = View(this).apply {
                val size = 24
                val dp = (size * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(dp, dp).also { lp ->
                    lp.marginStart = 8; lp.marginEnd = 8
                }
                background = ContextCompat.getDrawable(this@PinAuthActivity, R.drawable.dot_empty)
            }
            pinDots.add(dot)
            container.addView(dot)
        }
    }

    private fun updateDots(pin: String) {
        pinDots.forEachIndexed { i, dot ->
            dot.background = if (i < pin.length)
                ContextCompat.getDrawable(this, R.drawable.dot_filled)
            else
                ContextCompat.getDrawable(this, R.drawable.dot_empty)
        }
    }

    private fun setupNumpad() {
        val btns = mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2",
            R.id.btn_3 to "3", R.id.btn_4 to "4", R.id.btn_5 to "5",
            R.id.btn_6 to "6", R.id.btn_7 to "7", R.id.btn_8 to "8",
            R.id.btn_9 to "9"
        )
        btns.forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { appendDigit(digit) }
        }
        findViewById<Button>(R.id.btn_delete).setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
                updateDots(enteredPin)
            }
        }
        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            enteredPin = ""
            updateDots("")
        }
    }

    private fun appendDigit(digit: String) {
        if (enteredPin.length >= 8) return
        enteredPin += digit
        updateDots(enteredPin)

        if (enteredPin.length >= 4 && enteredPin == prefs.pin) {
            onAuthSuccess()
        } else if (enteredPin.length >= prefs.pin.length) {
            if (enteredPin != prefs.pin) onAuthFail()
        }
    }

    private fun onAuthSuccess() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun onAuthFail() {
        failCount++
        enteredPin = ""
        updateDots("")
        val errorTv = findViewById<TextView>(R.id.tv_error)
        errorTv.text = "PIN không đúng. Thử lại! ($failCount/5)"
        errorTv.visibility = View.VISIBLE

        if (failCount >= 5) {
            errorTv.text = "Quá nhiều lần thử sai!"
        }
    }

    private fun showSecurityQuestionDialog() {
        val input = android.widget.EditText(this).apply {
            hint = prefs.securityQuestion
            setPadding(32, 16, 32, 16)
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }

        AlertDialog.Builder(this, R.style.DarkDialog)
            .setTitle(prefs.securityQuestion)
            .setView(input)
            .setPositiveButton("Xác nhận") { _, _ ->
                val answer = input.text.toString().trim().lowercase()
                if (answer == prefs.securityAnswer) {
                    onAuthSuccess()
                } else {
                    val tv = findViewById<TextView>(R.id.tv_error)
                    tv.text = "Câu trả lời không đúng!"
                    tv.visibility = View.VISIBLE
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }
}
