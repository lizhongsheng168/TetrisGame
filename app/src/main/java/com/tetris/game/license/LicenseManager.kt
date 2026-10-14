package com.tetris.game.license

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

private val Context.licenseDataStore by preferencesDataStore(name = "license_store")

/**
 * 授权管理器 — 纯离线 RSA 验签（SHA256withRSA）
 *
 * CDKey 明文格式(验签原文):  deviceId|expireAtMillis
 *   - deviceId: DeviceIdProvider.getDeviceId 得到的 ANDROID_ID
 *   - expireAtMillis: 到期时间戳(毫秒, UTC)，例如 1893456000000
 * CDKey 传输格式:  Base64Url(raw) + "." + Base64Url(signature)
 *   - raw = "deviceId|expireAtMillis" 的 UTF-8
 *   - signature = RSA-SHA256(raw) 用私钥签名，Base64Url 编码
 *
 * 校验流程:
 *   1) 分割 CDKey 为 dataB64.sigB64
 *   2) Base64Url 解码得 raw 与 sigBytes
 *   3) 用内置公钥验签 raw
 *   4) 解析 deviceId 与 expireAt 校验绑定与有效期
 */
object LicenseManager {

    // ====== 内置公钥（与离线生成工具配对，修改需重新生成 CDKey）======
    // 对应私钥仅保存在你本地的 tools/cdkey-generator，不要提交到仓库
    private const val PUBLIC_KEY_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkoLZ/fhlTT3MIQJuuY//IqGUXlZHZ+5PVOnmuNl1AR2wygqm197cumDDpRUM8Q5xKaw3NnqAIErbQaFcp+D+SPglamCVPgFFGVsdeXPskV/NFUyuRCcorWkesPji1aXOLyi5ZstcE3+euU1vpf5FVBOZf2xjZ8ZGX2vBvdJZq66IbPSAsmfajDG+sxC9vuqSYE/gJqVw/treEo+TC1TpBP7THXjcZc6K1PwaQm4st1QQ4KTLz/hictbGo11BFjJZNM5qQ2H4K4RqaOmba8IJn+r+PNyZlZPVNwr3iqLfarVatXIFOTTk2yGmxHl23w3iJ5EyQtB0puoY0z4Edm9rLQIDAQAB"

    private object Keys {
        val CDKEY = stringPreferencesKey("license_cdkey")
        val EXPIRE_AT = longPreferencesKey("license_expire_at")
        val DEVICE_ID = stringPreferencesKey("license_device_id")
    }

    sealed class VerifyResult {
        data object Empty : VerifyResult()
        data class Valid(val expireAt: Long, val remainDays: Long) : VerifyResult()
        data object BadFormat : VerifyResult()
        data object BadSignature : VerifyResult()
        data object DeviceMismatch : VerifyResult()
        data object Expired : VerifyResult()
    }

    // ——— 对外：DataStore 状态流 ———

    fun cdkeyFlow(context: Context): Flow<String> =
        context.licenseDataStore.data.map { it[Keys.CDKEY] ?: "" }

    /** 内存/磁盘中的已保存授权是否有效（绑定当前设备且未过期） */
    suspend fun isLicensed(context: Context): Boolean {
        val cdkey = context.licenseDataStore.data.first()[Keys.CDKEY] ?: ""
        if (cdkey.isBlank()) return false
        val result = verify(context, cdkey)
        return result is VerifyResult.Valid
    }

    fun verifyFlow(context: Context): Flow<VerifyResult> =
        context.licenseDataStore.data.map { prefs ->
            val cdkey = prefs[Keys.CDKEY] ?: ""
            if (cdkey.isBlank()) VerifyResult.Empty else verify(context, cdkey)
        }

    // ——— 校验核心（离线） ———

    fun verify(context: Context, cdkey: String): VerifyResult {
        val trimmed = cdkey.trim()
        if (trimmed.isEmpty()) return VerifyResult.Empty
        val dot = trimmed.indexOf('.')
        if (dot <= 0 || dot >= trimmed.length - 1) return VerifyResult.BadFormat

        val dataB64 = trimmed.substring(0, dot).trim()
        val sigB64 = trimmed.substring(dot + 1).trim()
        val raw: String
        val sig: ByteArray
        try {
            raw = String(base64UrlDecode(dataB64), Charsets.UTF_8)
            sig = base64UrlDecode(sigB64)
        } catch (_: Exception) {
            return VerifyResult.BadFormat
        }

        // 验签
        try {
            val pubBytes = Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT)
            val spec = X509EncodedKeySpec(pubBytes)
            val pubKey = KeyFactory.getInstance("RSA").generatePublic(spec)
            val verifier = Signature.getInstance("SHA256withRSA")
            verifier.initVerify(pubKey)
            verifier.update(raw.toByteArray(Charsets.UTF_8))
            if (!verifier.verify(sig)) return VerifyResult.BadSignature
        } catch (_: Exception) {
            return VerifyResult.BadSignature
        }

        // 解析 raw: deviceId|expireAt
        val sep = raw.lastIndexOf('|')
        if (sep <= 0) return VerifyResult.BadFormat
        val boundDevice = raw.substring(0, sep)
        val expireAt = raw.substring(sep + 1).toLongOrNull() ?: return VerifyResult.BadFormat

        // 设备绑定校验
        val currentDevice = DeviceIdProvider.getDeviceId(context)
        if (boundDevice != currentDevice) return VerifyResult.DeviceMismatch

        // 有效期校验
        val now = System.currentTimeMillis()
        if (now > expireAt) return VerifyResult.Expired

        val remainDays = ((expireAt - now) / (24 * 3600 * 1000L)).coerceAtLeast(0)
        return VerifyResult.Valid(expireAt, remainDays)
    }

    // ——— 写入/清除 ———

    suspend fun saveCdkey(context: Context, cdkey: String) {
        val result = verify(context, cdkey)
        if (result !is VerifyResult.Valid) throw IllegalArgumentException(result.toString())
        // 解析 expire/device 存一份便于展示
        val raw = String(base64UrlDecode(cdkey.substringBefore('.')), Charsets.UTF_8)
        val sep = raw.lastIndexOf('|')
        val device = raw.substring(0, sep)
        val expireAt = raw.substring(sep + 1).toLong()
        context.licenseDataStore.edit { prefs ->
            prefs[Keys.CDKEY] = cdkey.trim()
            prefs[Keys.EXPIRE_AT] = expireAt
            prefs[Keys.DEVICE_ID] = device
        }
    }

    suspend fun clear(context: Context) {
        context.licenseDataStore.edit { prefs ->
            prefs.remove(Keys.CDKEY)
            prefs.remove(Keys.EXPIRE_AT)
            prefs.remove(Keys.DEVICE_ID)
        }
    }

    // ——— 工具 ———

    internal fun base64UrlEncode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    internal fun base64UrlDecode(s: String): ByteArray =
        Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP)

    /** 用于调试：当前设备ID */
    fun currentDeviceId(context: Context): String = DeviceIdProvider.getDeviceId(context)
}
