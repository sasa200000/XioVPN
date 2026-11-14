package com.xiovpn.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.xiovpn.mobile.core.ConnectionState
import com.xiovpn.mobile.ui.ProvideFixedFontScale

object XioColors {
    val BackgroundTop = Color(0xFF04080F)
    val BackgroundMiddle = Color(0xFF071019)
    val BackgroundBottom = Color(0xFF0A1622)
    val Surface = Color(0xFF0E1A26)
    val DisconnectedBlue = Color(0xFF2FA8E0)
    val ConnectingCyan = Color(0xFF39D9E8)
    val ConnectedGreen = Color(0xFF3CF0AF)
    val DisconnectingAmber = Color(0xFFFFC061)
    val ErrorRed = Color(0xFFFF5C7A)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF94A6B8)
    val CardBorder = Color(0x20FFFFFF)
    val Divider = Color(0x24FFFFFF)
    val ButtonCenter = Color(0xFF14242F)
    val ButtonEdge = Color(0xFF060F16)
    val ButtonInnerRing = Color(0xFF3E5F63)
}

data class UiStateColors(val accent: Color)

fun colorsFor(state: ConnectionState): UiStateColors = when (state) {
    ConnectionState.DISCONNECTED -> UiStateColors(XioColors.DisconnectedBlue)
    ConnectionState.CONNECTING -> UiStateColors(XioColors.ConnectingCyan)
    ConnectionState.CONNECTED -> UiStateColors(XioColors.ConnectedGreen)
    ConnectionState.DISCONNECTING -> UiStateColors(XioColors.DisconnectingAmber)
    ConnectionState.ERROR -> UiStateColors(XioColors.ErrorRed)
}

private val UacDarkColorScheme = darkColorScheme(
    primary = XioColors.ConnectedGreen,
    secondary = XioColors.DisconnectedBlue,
    background = XioColors.BackgroundTop,
    surface = XioColors.Surface,
    onPrimary = XioColors.BackgroundTop,
    onBackground = XioColors.TextPrimary,
    onSurface = XioColors.TextPrimary,
    error = XioColors.ErrorRed,
)

@Composable
fun XioVpnTheme(content: @Composable () -> Unit) {
    ProvideFixedFontScale {
        MaterialTheme(
            colorScheme = UacDarkColorScheme,
            content = content,
        )
    }
}
