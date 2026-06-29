package com.privacyphone.ui

import android.app.admin.DevicePolicyManager
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

class SetupActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences
    private var currentPin = ""
    private var confirmedPin = ""
    private var isConfirming = false
    private val pinDots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        if (prefs.isSetupDone) {
            goToAuth()
            return
        }

        setContentView(R.layout.activity_setup)
        setupPinDots()
        setupNumpad()
        updateStepLabel()
    }

    private fun goToAuth() {
        startActivity(Intent(this, PinAuthActivity::class.java))
        finish()
    }

    private fun setupPinDots() {
        val container = findViewById<LinearLayout>(R.id.pin_dots_container)
        container.removeAllViews()
        pinDots.clear()
        val dpSize = (24 * resources.displayMetrics.density).toInt()
        repeat(8) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpSize, dpSize).also { lp ->
                    lp.marginStart = 8; lp.marginEnd = 8
                }
                background = ContextCompat.getDrawable(this@SetupActivity, R.drawable.dot_empty)
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
        mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2",
            R.id.btn_3 to "3", R.id.btn_4 to "4", R.id.btn_5 to "5",
            R.id.btn_6 to "6", R.id.btn_7 to "7", R.id.btn_8 to "8",
            R.id.btn_9 to "9"
        ).forEach { (id, d) -> findViewById<Button>(id).setOnClickListener { addDigit(d) } }

        findViewById<Button>(R.id.btn_delete).setOnClickListener { deleteDigit() }
        findViewById<Button>(R.id.btn_clear).setOnClickListener { clearPin() }
        findViewById<Button>(R.id.btn_confirm).setOnClickListener { handleConfirm() }
        findViewById<TextView>(R.id.tv_security_question).setOnClickListener {
            showSecurityQuestionDialog()
        }
    }

    private fun addDigit(digit: String) {
        val pin = if (isConfirming) confirmedPin else currentPin
        if (pin.length >= 8) return

        if (isConfirming) {
            confirmedPin += digit
            updateDots(confirmedPin)
            if (confirmedPin.length >= 4) showConfirmBtn()
        } else {
            currentPin += digit
            updateDots(currentPin)
            if (currentPin.length >= 4) showConfirmBtn()
        }
        clearError()
    }

    private fun deleteDigit() {
        if (isConfirming) {
            if (confirmedPin.isNotEmpty()) confirmedPin = confirmedPin.dropLast(1)
            updateDots(confirmedPin)
            if (confirmedPin.length < 4) hideConfirmBtn()
        } else {
            if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1)
            updateDots(currentPin)
            if (currentPin.length < 4) hideConfirmBtn()
        }
    }

    private fun clearPin() {
        if (isConfirming) { confirmedPin = ""; updateDots("") }
        else { currentPin = ""; updateDots("") }
        hideConfirmBtn()
    }

    private fun showConfirmBtn() { findViewById<Button>(R.id.btn_confirm).visibility = View.VISIBLE }
    private fun hideConfirmBtn() { findViewById<Button>(R.id.btn_confirm).visibility = View.GONE }

    private fun handleConfirm() {
        if (!isConfirming) {
            if (currentPin.length < 4) { showError(getString(R.string.pin_too_short)); return }
            isConfirming = true
            confirmedPin = ""
            updateDots("")
            hideConfirmBtn()
            updateStepLabel()
        } else {
            if (confirmedPin == currentPin) {
                finishSetup()
            } else {
                showError(getString(R.string.pin_mismatch))
                confirmedPin = ""
                updateDots("")
                hideConfirmBtn()
            }
        }
    }

    private fun updateStepLabel() {
        val tv = findViewById<TextView>(R.id.tv_step_label)
        tv.text = if (isConfirming) "Nhập lại mã PIN để xác nhận" else "Nhập mã PIN (ít nhất 4 chữ số)"
    }

    private fun showError(msg: String) {
        val tv = findViewById<TextView>(R.id.tv_error)
        tv.text = msg; tv.visibility = View.VISIBLE
    }

    private fun clearError() {
        findViewById<TextView>(R.id.tv_error).visibility = View.INVISIBLE
    }

    private fun finishSetup() {
        prefs.pin = currentPin
        prefs.isSetupDone = true
        // Go to permissions screen
        startActivity(Intent(this, PermissionsSetupActivity::class.java))
        finish()
    }

    private fun showSecurityQuestionDialog() {
        val questions = arrayOf(
            "Tên thú cưng đầu tiên của bạn?",
            "Tên trường tiểu học của bạn?",
            "Tên mẹ của bạn?",
            "Ngày sinh của bạn?",
            "Thành phố bạn sinh ra?"
        )
        AlertDialog.Builder(this, R.style.DarkDialog)
            .setTitle("Chọn câu hỏi bảo mật")
            .setItems(questions) { _, which -> showAnswerDialog(questions[which]) }
            .show()
    }

    private fun showAnswerDialog(question: String) {
        val input = android.widget.EditText(this).apply {
            hint = "Câu trả lời của bạn"
            setPadding(48, 24, 48, 24)
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setBackgroundColor(ContextCompat.getColor(context, R.color.surface_dark))
        }
        AlertDialog.Builder(this, R.style.DarkDialog)
            .setTitle(question)
            .setView(input)
            .setPositiveButton("Xác nhận") { _, _ ->
                val ans = input.text.toString().trim()
                if (ans.isNotEmpty()) {
                    prefs.securityQuestion = question
                    prefs.securityAnswer = ans.lowercase()
                    if (currentPin.length >= 4) {
                        prefs.pin = currentPin
                        prefs.isSetupDone = true
                        startActivity(Intent(this, PermissionsSetupActivity::class.java))
                        finish()
                    } else {
                        showError("Bạn cũng cần tạo mã PIN!")
                    }
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }
}
