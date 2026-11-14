package com.xiovpn.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiovpn.mobile.ui.theme.XioColors
import com.xiovpn.mobile.vpn.MonthlyTrafficStore
import com.xiovpn.mobile.vpn.TrafficStatsStore
import java.util.Locale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiovpn.mobile.core.ConnectionState
import com.xiovpn.mobile.core.ConnectionStateStore
import com.xiovpn.mobile.settings.GlowSettingsStore
import kotlinx.coroutines.delay

@Composable
internal fun TrafficStatsRow(
    accent: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val stats by TrafficStatsStore.stats.collectAsState()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
    ) {
        TrafficStatCard(
            title = homeText("Download", "دانلود"),
            totalBytes = stats.downloadBytes,
            bytesPerSecond = stats.downloadBytesPerSecond,
            icon = Icons.Rounded.ArrowDownward,
            accent = accent,
            compact = compact,
            modifier = Modifier.weight(1f),
        )
        TrafficStatCard(
            title = homeText("Upload", "آپلود"),
            totalBytes = stats.uploadBytes,
            bytesPerSecond = stats.uploadBytesPerSecond,
            icon = Icons.Rounded.ArrowUpward,
            accent = accent,
            compact = compact,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TrafficStatCard(
    title: String,
    totalBytes: Long,
    bytesPerSecond: Long,
    icon: ImageVector,
    accent: Color,
    compact: Boolean,
    modifier: Modifier,
) {
    val localizedFont = homeLocalizedFont()
    val shape = RoundedCornerShape(if (compact) 18.dp else 21.dp)
    val total = formatBytes(totalBytes)
    val rate = formatBytes(bytesPerSecond)

    // Live border light + wave history. The colour steps once per second while
    // the tunnel is up, matching the Connect button's rim light. The user's Glow
    // settings drive shape/thickness; "off" leaves a static border.
    val context = LocalContext.current
    val glowStore = remember(context) { GlowSettingsStore.get(context) }
    val glow by glowStore.settings.collectAsStateWithLifecycle()
    val connectedState by ConnectionStateStore.state.collectAsStateWithLifecycle()
    val isConnected = connectedState == ConnectionState.CONNECTED
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(glow.enabled, isConnected) {
        if (glow.enabled) {
            while (true) {
                delay(1_000)
                tick++
            }
        }
    }
    val history = remember { mutableStateListOf<Float>() }
    LaunchedEffect(bytesPerSecond) {
        history.add(bytesPerSecond.toFloat())
        if (history.size > 28) history.removeAt(0)
    }
    val borderColor = glow.colorArgb?.let { Color(it) } ?: accent
    val cardShape = RoundedCornerShape(if (compact) 18.dp else 21.dp)

    Box(
        modifier = modifier.height(if (compact) 92.dp else 108.dp),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(Color(0xE512202E), Color(0xB5091724))),
                cardShape,
            )
            .padding(horizontal = if (compact) 8.dp else 11.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (compact) 28.dp else 32.dp)
                    .background(accent.copy(alpha = 0.11f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.24f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(if (compact) 16.dp else 19.dp))
            }
            Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
            Text(
                title,
                color = XioColors.TextSecondary,
                fontSize = if (compact) 10.5.sp else 12.sp,
                fontFamily = localizedFont,
                fontWeight = if (LocalHomePersian.current) FontWeight.Medium else null,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                total.amount,
                color = borderColor,
                fontSize = if (compact) 17.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                total.unit,
                color = XioColors.TextSecondary,
                fontSize = if (compact) 9.sp else 10.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 2.dp),
                maxLines = 1,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "${rate.amount} ${rate.unit}/s",
                color = XioColors.TextSecondary.copy(alpha = 0.82f),
                fontSize = if (compact) 8.5.sp else 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        // Live sea-wave chart: moves every frame, recolours every second.
        SeaWaveChart(
            values = history.toList(),
            accent = borderColor,
            connected = isConnected,
            motionEnabled = glow.enabled,
            height = if (compact) 22.dp else 26.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    // Border light dance around the whole card, matching the Connect button.
    if (glow.enabled) {
        BorderLightDance(
            tick = tick,
            accent = borderColor,
            connected = isConnected,
            motionEnabled = glow.enabled,
            cornerRadius = if (compact) 18.dp else 21.dp,
            thickness = if (compact) 1.8.dp else 2.2.dp,
            modifier = Modifier.matchParentSize(),
        )
    } else {
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, borderColor.copy(alpha = 0.44f), cardShape),
        )
    }
    }
}

@Composable
internal fun TrafficUsagePanels() {
    val context = LocalContext.current
    val stats by TrafficStatsStore.stats.collectAsState()
    val monthly by remember(context) { MonthlyTrafficStore.get(context) }.usage.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TrafficPeriodPanel(
            title = homeText("This session", "این جلسه"),
            downloadBytes = stats.downloadBytes,
            uploadBytes = stats.uploadBytes,
        )
        TrafficPeriodPanel(
            title = homeText("This month", "این ماه"),
            downloadBytes = monthly.downloadBytes,
            uploadBytes = monthly.uploadBytes,
        )
    }
}

internal fun monthlyTrafficLabel(totalBytes: Long): String = formatBytesLabel(totalBytes)

@Composable
private fun TrafficPeriodPanel(
    title: String,
    downloadBytes: Long,
    uploadBytes: Long,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE512202E), shape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(XioColors.ConnectingCyan),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                color = XioColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = homeLocalizedFont(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TrafficDirectionValue(
                    icon = Icons.Rounded.ArrowDownward,
                    bytes = downloadBytes,
                    modifier = Modifier.weight(1f),
                )
                TrafficDirectionValue(
                    icon = Icons.Rounded.ArrowUpward,
                    bytes = uploadBytes,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TrafficDirectionValue(
    icon: ImageVector,
    bytes: Long,
    modifier: Modifier,
) {
    val formatted = formatBytes(bytes)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = XioColors.ConnectingCyan, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            formatted.amount,
            color = XioColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            formatted.unit,
            color = XioColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private data class FormattedBytes(val amount: String, val unit: String)

private fun formatBytesLabel(bytes: Long): String {
    val formatted = formatBytes(bytes)
    return "${formatted.amount} ${formatted.unit}"
}

private fun formatBytes(bytes: Long): FormattedBytes {
    val value = bytes.coerceAtLeast(0L).toDouble()
    return when {
        value >= 1_073_741_824.0 -> FormattedBytes(
            String.format(Locale.US, if (value < 10_737_418_240.0) "%.2f" else "%.1f", value / 1_073_741_824.0),
            "GB",
        )
        value >= 1_048_576.0 -> FormattedBytes(
            String.format(Locale.US, if (value < 104_857_600.0) "%.1f" else "%.0f", value / 1_048_576.0),
            "MB",
        )
        value >= 1_024.0 -> FormattedBytes(
            String.format(Locale.US, if (value < 102_400.0) "%.1f" else "%.0f", value / 1_024.0),
            "KB",
        )
        else -> FormattedBytes(value.toLong().toString(), "B")
    }
}
