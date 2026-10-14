package com.tetris.game.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.license.DeviceIdProvider
import com.tetris.game.license.LicenseManager
import com.tetris.game.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LicenseScreen(
    onActivated: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deviceId = remember { DeviceIdProvider.getDeviceId(context) }
    var cdkeyInput by remember { mutableStateOf("") }
    var verifying by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // 已保存的授权状态
    val verifyState by LicenseManager.verifyFlow(context).collectAsState(initial = LicenseManager.VerifyResult.Empty)
    val savedCdkey by LicenseManager.cdkeyFlow(context).collectAsState(initial = "")

    // 预填已保存的 cdkey
    LaunchedEffect(savedCdkey) {
        if (savedCdkey.isNotBlank() && cdkeyInput.isBlank()) cdkeyInput = savedCdkey
    }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onBack != null) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("返回", fontSize = 14.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
            }
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(56.dp))
            Text("授权激活", fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextPrimary, modifier = Modifier.padding(top = 12.dp))
            Text(
                "本机需绑定 CDKey 后方可进入游戏\n请将下方设备序列号发给管理员生成 CDKey",
                fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp), lineHeight = 18.sp
            )

            Spacer(Modifier.height(20.dp))

            // 设备序列号卡片
            Surface(shape = RoundedCornerShape(16.dp), color = BoardBackground, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("设备序列号（ANDROID_ID）", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("deviceId", deviceId))
                            Toast.makeText(context, "已复制设备序列号", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "复制", tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                        }
                    }
                    SelectionContainer {
                        Text(
                            deviceId, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BoardCardBackground)
                                .padding(12.dp)
                        )
                    }
                    Text("长按可选择复制，发给管理员生成对应 CDKey（有效期按天）", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
                }
            }

            // 当前授权状态
            when (val s = verifyState) {
                is LicenseManager.VerifyResult.Valid -> {
                    Surface(shape = RoundedCornerShape(12.dp), color = PrimaryAccent.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✓ 已授权", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PrimaryAccent)
                            Text("到期：${sdf.format(Date(s.expireAt))}  ·  剩余 ${s.remainDays} 天", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                is LicenseManager.VerifyResult.Expired -> {
                    Text("当前 CDKey 已过期，请重新生成", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
                is LicenseManager.VerifyResult.DeviceMismatch -> {
                    Text("CDKey 与本机不匹配（已绑定其他设备）", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
                is LicenseManager.VerifyResult.BadSignature -> {
                    Text("CDKey 签名无效", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
                is LicenseManager.VerifyResult.BadFormat -> {
                    Text("CDKey 格式错误", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
                else -> {}
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = cdkeyInput,
                onValueChange = { cdkeyInput = it.trim(); message = null },
                label = { Text("粘贴 CDKey") },
                placeholder = { Text("Base64Url.raw.Base64Url.sig", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = false,
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    disabledTextColor = TextPrimary.copy(alpha = 0.6f),
                    cursorColor = PrimaryAccent,
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = BoardBorder,
                    focusedLabelColor = PrimaryAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedPlaceholderColor = TextMuted,
                    unfocusedPlaceholderColor = TextMuted,
                    focusedContainerColor = BoardCardBackground,
                    unfocusedContainerColor = BoardCardBackground,
                    disabledContainerColor = BoardCardBackground
                )
            )

            if (message != null) {
                Text(
                    message!!, fontSize = 12.sp,
                    color = if (isError) MaterialTheme.colorScheme.error else PrimaryAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    if (cdkeyInput.isBlank()) {
                        message = "请先粘贴 CDKey"; isError = true; return@Button
                    }
                    verifying = true
                    scope.launch {
                        val result = LicenseManager.verify(context, cdkeyInput)
                        when (result) {
                            is LicenseManager.VerifyResult.Valid -> {
                                try {
                                    LicenseManager.saveCdkey(context, cdkeyInput)
                                    message = "激活成功！剩余 ${result.remainDays} 天"
                                    isError = false
                                    Toast.makeText(context, "激活成功", Toast.LENGTH_SHORT).show()
                                    onActivated()
                                } catch (e: Exception) {
                                    message = "保存失败：${e.message}"; isError = true
                                }
                            }
                            is LicenseManager.VerifyResult.Empty -> { message = "CDKey 为空"; isError = true }
                            is LicenseManager.VerifyResult.BadFormat -> { message = "格式错误，请检查是否完整复制"; isError = true }
                            is LicenseManager.VerifyResult.BadSignature -> { message = "签名无效，CDKey 可能被篡改或密钥不匹配"; isError = true }
                            is LicenseManager.VerifyResult.DeviceMismatch -> { message = "设备不匹配：该 CDKey 绑定了其他设备"; isError = true }
                            is LicenseManager.VerifyResult.Expired -> { message = "CDKey 已过期，请联系管理员重新生成"; isError = true }
                        }
                        verifying = false
                    }
                },
                enabled = !verifying,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (verifying) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TextPrimary)
                else Text(if (verifyState is LicenseManager.VerifyResult.Valid) "重新激活" else "激活并进入", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (verifyState is LicenseManager.VerifyResult.Valid) {
                TextButton(onClick = {
                    scope.launch {
                        // 仅展示，不自动跳转
                    }
                    onActivated()
                }, modifier = Modifier.padding(top = 4.dp)) {
                    Text("已激活，直接进入游戏", color = PrimaryAccent, fontSize = 13.sp)
                }
            }

            Text(
                "离线校验 · 按天有效期 · 换机需重新生成",
                fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
