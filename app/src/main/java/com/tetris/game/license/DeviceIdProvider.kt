package com.tetris.game.license

import android.content.Context
import android.provider.Settings

/**
 * 设备序列号提供器 — 以 ANDROID_ID 为主
 * ANDROID_ID：无需权限，恢复出厂后会变，符合 Google 推荐
 */
object DeviceIdProvider {

    fun getDeviceId(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) { null }

        // ANDROID_ID 在模拟器/部分设备上为 9774d56d682e549c 需视为无效
        if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c" && androidId != "unknown") {
            return androidId.trim()
        }
        // 极端兜底：用 ANDROID_ID 的替代指纹（不依赖 READ_PHONE_STATE）
        return try {
            val fp = android.os.Build.FINGERPRINT ?: ""
            val model = android.os.Build.MODEL ?: ""
            // 简单稳定哈希，避免空
            "fb-" + (fp + model).hashCode().toString().replace("-", "x")
        } catch (_: Exception) {
            "unknown-device"
        }
    }
}
