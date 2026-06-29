package com.privacyphone.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.privacyphone.R
import com.privacyphone.data.prefs.AppPreferences
import com.privacyphone.ui.PermissionsSetupActivity
import com.privacyphone.ui.SetupActivity
import com.privacyphone.util.PermissionsHelper

class SettingsFragment : Fragment() {

    private lateinit var prefs: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPreferences(requireContext())

        // Change PIN
        view.findViewById<Button>(R.id.btn_change_pin).setOnClickListener {
            showChangePinDialog()
        }

        // Permissions
        view.findViewById<Button>(R.id.btn_permissions).setOnClickListener {
            startActivity(Intent(requireContext(), PermissionsSetupActivity::class.java))
        }

        // Device admin status
        updateAdminStatus(view)

        view.findViewById<Button>(R.id.btn_toggle_admin).setOnClickListener {
            if (PermissionsHelper.isDeviceAdminActive(requireContext())) {
                showDisableAdminDialog()
            } else {
                PermissionsHelper.requestDeviceAdmin(requireActivity())
            }
        }

        // Reset security
        view.findViewById<Button>(R.id.btn_reset_security).setOnClickListener {
            showResetDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { updateAdminStatus(it) }
    }

    private fun updateAdminStatus(view: View) {
        val isAdmin = PermissionsHelper.isDeviceAdminActive(requireContext())
        val statusTv = view.findViewById<TextView>(R.id.tv_admin_status)
        val toggleBtn = view.findViewById<Button>(R.id.btn_toggle_admin)

        if (isAdmin) {
            statusTv.text = "✅ Bảo vệ khỏi gỡ cài đặt: BẬT"
            statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
            toggleBtn.text = "Tắt bảo vệ"
            toggleBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.accent_red)
        } else {
            statusTv.text = "⚠️ Bảo vệ khỏi gỡ cài đặt: TẮT"
            statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_red))
            toggleBtn.text = "Bật bảo vệ"
            toggleBtn.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.accent_green)
        }
    }

    private fun showChangePinDialog() {
        // First verify current PIN
        showVerifyCurrentPin {
            showNewPinDialog()
        }
    }

    private fun showVerifyCurrentPin(onVerified: () -> Unit) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nhập PIN hiện tại"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(48, 24, 48, 24)
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        AlertDialog.Builder(requireContext(), R.style.DarkDialog)
            .setTitle("Xác minh PIN hiện tại")
            .setView(input)
            .setPositiveButton("Xác nhận") { _, _ ->
                if (input.text.toString() == prefs.pin) onVerified()
                else showError("PIN không đúng!")
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showNewPinDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nhập PIN mới (ít nhất 4 chữ số)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(48, 24, 48, 24)
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        AlertDialog.Builder(requireContext(), R.style.DarkDialog)
            .setTitle("PIN mới")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newPin = input.text.toString()
                if (newPin.length >= 4) {
                    prefs.pin = newPin
                    showSuccess("PIN đã được cập nhật!")
                } else {
                    showError("PIN phải có ít nhất 4 chữ số!")
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showDisableAdminDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nhập PIN để xác nhận"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(48, 24, 48, 24)
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        AlertDialog.Builder(requireContext(), R.style.DarkDialog)
            .setTitle("⚠️ Tắt bảo vệ gỡ cài đặt")
            .setMessage("Nhập PIN để tắt tính năng bảo vệ")
            .setView(input)
            .setPositiveButton("Xác nhận") { _, _ ->
                if (input.text.toString() == prefs.pin) {
                    PermissionsHelper.revokeDeviceAdmin(requireContext())
                    view?.let { updateAdminStatus(it) }
                } else {
                    showError("PIN không đúng!")
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showResetDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nhập PIN hiện tại"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(48, 24, 48, 24)
            setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }
        AlertDialog.Builder(requireContext(), R.style.DarkDialog)
            .setTitle("⚠️ Đặt lại bảo mật")
            .setMessage("Hành động này sẽ xóa tất cả cài đặt bảo mật và yêu cầu thiết lập lại.")
            .setView(input)
            .setPositiveButton("Đặt lại") { _, _ ->
                if (input.text.toString() == prefs.pin) {
                    prefs.pin = ""
                    prefs.isSetupDone = false
                    prefs.securityQuestion = ""
                    prefs.securityAnswer = ""
                    PermissionsHelper.revokeDeviceAdmin(requireContext())
                    startActivity(Intent(requireContext(), SetupActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    showError("PIN không đúng!")
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun showError(msg: String) {
        AlertDialog.Builder(requireContext(), R.style.DarkDialog)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccess(msg: String) {
        AlertDialog.Builder(requireContext(), R.style.DarkDialog)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }
}
