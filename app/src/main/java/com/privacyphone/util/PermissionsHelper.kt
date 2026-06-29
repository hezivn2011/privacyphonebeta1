package com.privacyphone.util

import android.app.Activity
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.privacyphone.receiver.PrivacyDeviceAdminReceiver

object PermissionsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return UsageStatsHelper.hasUsagePermission(context)
    }

    fun hasAccessibilityPermission(context: Context): Boolean {
        val serviceName = "${context.packageName}/.service.AppBlockerAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(serviceName)
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        return PrivacyDeviceAdminReceiver.isAdminActive(context)
    }

    fun openUsageAccessSettings(activity: Activity) {
        activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openAccessibilitySettings(activity: Activity) {
        activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openOverlaySettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}"))
        activity.startActivity(intent)
    }

    fun requestDeviceAdmin(activity: Activity) {
        val component = PrivacyDeviceAdminReceiver.getComponentName(activity)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "PrivacyPhone cần quyền quản trị thiết bị để ngăn bị gỡ cài đặt mà không có PIN.")
        }
        activity.startActivityForResult(intent, 9001)
    }

    fun revokeDeviceAdmin(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        dpm.removeActiveAdmin(PrivacyDeviceAdminReceiver.getComponentName(context))
    }

    /**
     * Returns list of missing critical permissions
     */
    fun getMissingPermissions(context: Context): List<PermissionItem> {
        val missing = mutableListOf<PermissionItem>()
        if (!hasUsageStatsPermission(context))
            missing.add(PermissionItem.USAGE_STATS)
        if (!hasAccessibilityPermission(context))
            missing.add(PermissionItem.ACCESSIBILITY)
        if (!isDeviceAdminActive(context))
            missing.add(PermissionItem.DEVICE_ADMIN)
        return missing
    }

    enum class PermissionItem(val title: String, val desc: String) {
        USAGE_STATS("Quyền truy cập sử dụng", "Cần để theo dõi thời gian dùng app"),
        ACCESSIBILITY("Dịch vụ hỗ trợ tiếp cận", "Cần để chặn app khi hết giờ"),
        DEVICE_ADMIN("Quản trị thiết bị", "Cần để ngăn gỡ cài đặt không có PIN"),
        OVERLAY("Hiển thị trên app khác", "Cần để hiện màn hình chặn")
    }
}
