package com.privacyphone.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.privacyphone.ui.PinAuthActivity

class PrivacyDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, PrivacyDeviceAdminReceiver::class.java)
        }

        fun isAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isAdminActive(getComponentName(context))
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "PrivacyPhone: Bảo vệ đã được kích hoạt", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Bạn cần nhập mã PIN để tắt bảo vệ PrivacyPhone!"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Bảo vệ đã bị tắt", Toast.LENGTH_SHORT).show()
    }
}
