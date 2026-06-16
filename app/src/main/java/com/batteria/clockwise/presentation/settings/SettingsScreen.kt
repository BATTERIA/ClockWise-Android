package com.batteria.clockwise.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.batteria.clockwise.presentation.clock.ClockViewModel
import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.VoiceGender
import com.batteria.clockwise.presentation.theme.BlueyPalette

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: ClockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isZh = state.language == Language.ZH
    Box(modifier = Modifier.fillMaxSize().background(BlueyPalette.Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isZh) "返回" else "Back")
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isZh) "设置" else "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueyPalette.Bluey,
                )
            }
            Spacer(Modifier.height(16.dp))

            // Voice gender section
            Text(
                text = if (isZh) "🎙️ 播报声音" else "🎙️ Voice",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BlueyPalette.Bluey,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isZh) "点击时钟旁的小喇叭时使用的声音" else "Used when you tap the speaker on the clock",
                fontSize = 12.sp,
                color = BlueyPalette.Bluey.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VoiceCard(
                    emoji = "👧",
                    label = if (isZh) "小姐姐" else "Girl",
                    description = if (isZh) "温柔活泼" else "Warm & lively",
                    selected = state.voiceGender == VoiceGender.GIRL,
                    onClick = { viewModel.setVoiceGender(VoiceGender.GIRL) },
                    modifier = Modifier.weight(1f),
                )
                VoiceCard(
                    emoji = "👦",
                    label = if (isZh) "小哥哥" else "Boy",
                    description = if (isZh) "阳光可爱" else "Sunny & cute",
                    selected = state.voiceGender == VoiceGender.BOY,
                    onClick = { viewModel.setVoiceGender(VoiceGender.BOY) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = if (isZh) "ℹ️ 提示：高品质语音仅在 12 小时制下使用。"
                                else "ℹ️ Tip: High-quality voice is used in 12-hour mode only.",
                fontSize = 12.sp,
                color = BlueyPalette.Bluey.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun VoiceCard(
    emoji: String,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) BlueyPalette.Bluey else Color.Transparent
    Surface(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) BlueyPalette.Bluey.copy(alpha = 0.15f) else Color.White,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(BlueyPalette.Bluey, CircleShape)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = emoji, fontSize = 36.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BlueyPalette.Bluey,
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = BlueyPalette.Bluey.copy(alpha = 0.7f),
                )
            }
        }
        // tiny invisible border via decoration: we can rely on tinted bg.
        @Suppress("UNUSED_VARIABLE") val _b = border
    }
}
