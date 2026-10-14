package com.tetris.game.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.game.data.SaveManager
import com.tetris.game.ui.theme.LocalAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialGateSheet(
    isLicensed: Boolean,
    trialRemain: Int,
    onDismiss: () -> Unit,
    onGoLicense: () -> Unit,
    onContinueTrial: () -> Unit
) {
    val theme = LocalAppTheme.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.boardCardBackground,
        contentColor = theme.textPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(theme.primaryAccentStrong, theme.secondaryAccent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(12.dp))
            if (isLicensed) {
                Text("已授权", fontSize = 18.sp, fontWeight = FontWeight.Black, color = theme.textPrimary)
                Text("当前设备已激活，畅玩无限制", fontSize = 13.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccentStrong)) {
                    Text("继续游戏", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (trialRemain > 0) {
                Text("试玩剩余 $trialRemain 局", fontSize = 18.sp, fontWeight = FontWeight.Black, color = theme.textPrimary)
                Text("试玩结束后需输入 CDKey 激活后继续游玩\n先体验，再决定是否激活", fontSize = 13.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center, lineHeight = 18.sp)
                Surface(shape = RoundedCornerShape(12.dp), color = theme.boardBackground, border = BorderStroke(1.dp, theme.boardBorder), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("试玩规则", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textMuted, letterSpacing = 0.8.sp)
                        Text("· 未激活时可免费游玩 ${SaveManager.TRIAL_LIMIT} 局（进入对局即计1局）\n· 试玩期内完整体验无尽与冒险\n· 达到上限后引导至授权页输入 CDKey", fontSize = 12.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 6.dp), lineHeight = 16.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onGoLicense, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("去激活", color = theme.textPrimary, fontWeight = FontWeight.Bold) }
                    Button(onClick = onContinueTrial, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccentStrong)) { Text("继续试玩", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            } else {
                Text("试玩已结束", fontSize = 18.sp, fontWeight = FontWeight.Black, color = theme.textPrimary)
                Text("已用完 ${SaveManager.TRIAL_LIMIT} 局免费试玩\n请输入 CDKey 激活后继续游玩", fontSize = 13.sp, color = theme.textSecondary, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onGoLicense, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccentStrong)) {
                    Text("去激活", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("设备序列号可在授权页一键复制发给管理员", fontSize = 11.sp, color = theme.textMuted, modifier = Modifier.padding(top = 10.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
