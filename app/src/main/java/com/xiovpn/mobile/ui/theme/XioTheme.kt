package com.xiovpn.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import com.xiovpn.mobile.core.ConnectionState
import com.xiovpn.mobile.ui.ProvideFixedFontScale

/**
 * Every colour token in the app.
 *
 * These used to be compile-time constants. They are now backed by
 * [AppearanceSettings], so each getter reads Compose snapshot state: the whole
 * UI re-themes when the user picks a different accent or surface style, with no
 * palette parameter threaded through the ~34 screens that read these tokens.
 *
 * Reads performed outside composition (for example a `val` in a top-level
 * property initialiser) take the current value and will not update; call sites
 * that need to track theme changes must read inside a composable.
 */
object XioColors {

    // ---- surfaces -------------------------------------------------------

    /** True when the user picked the pure-black OLED treatment. */
    val oled: Boolean get() = AppearanceSettings.style == ThemeStyle.OLED

    val BackgroundTop: Color
        get() = if (oled) Color(0xFF000000) else Color(0xFF070B16)

    val BackgroundMiddle: Color
        get() = if (oled) Color(0xFF000000) else Color(0xFF0A1120)

    val BackgroundBottom: Color
        get() = if (oled) Color(0xFF04070D) else Color(0xFF0D1526)

    val Surface: Color
        get() = if (oled) Color(0xFF111114) else Color(0xFF141D2E)

    /** Card fill sitting on [Surface]; used by the glass panels. */
    val GlassTop: Color
        get() = if (oled) Color(0xE51B1B20) else Color(0xC02B344A)

    val GlassBottom: Color
        get() = if (oled) Color(0xF2000000) else Color(0xD9111726)

    // ---- brand accent ---------------------------------------------------

    /** The accent the user picked; drives brand surfaces, not state. */
    val Accent: Color get() = AppearanceSettings.accent

    /** Muted accent for collected/hovered surfaces. */
    val AccentMuted: Color
        get() = lerp(Accent, BackgroundTop, 0.72f)

    // ---- connection state ----------------------------------------------
    // Green / amber / red stay semantic so state is still readable at a
    // glance; only the "idle" and "connecting" hues follow the accent.

    val DisconnectedBlue: Color get() = Accent

    val ConnectingCyan: Color
        get() = lerp(Accent, Color.White, 0.28f)

    val ConnectedGreen: Color get() = Color(0xFF3CF0AF)

    val DisconnectingAmber: Color get() = Color(0xFFFFC061)

    val ErrorRed: Color get() = Color(0xFFFF5C7A)

    // ---- text --------------------------------------------------------

    val TextPrimary: Color get() = Color(0xFFFFFFFF)

    val TextSecondary: Color
        get() = if (oled) Color(0xFF9AA3B2) else Color(0xFF94A6B8)

    // ---- strokes -----------------------------------------------------

    val CardBorder: Color
        get() = if (oled) Color(0x14FFFFFF) else Color(0x20FFFFFF)

    val Divider: Color
        get() = if (oled) Color(0x1AFFFFFF) else Color(0x24FFFFFF)

    /** Glass edge, tinted by the accent like the Dark TV cards. */
    val GlassBorder: Color
        get() = if (oled) Color(0x33FFFFFF) else lerp(Accent, Color.White, 0.55f).copy(alpha = 0.34f)

    // ---- connect button ------------------------------------------------

    val ButtonCenter: Color
        get() = if (oled) Color(0xFF1A1A20) else lerp(Accent, BackgroundMiddle, 0.86f)

    val ButtonEdge: Color
        get() = if (oled) Color(0xFF07070A) else Color(0xFF060F16)

    val ButtonInnerRing: Color
        get() = if (oled) Color(0xFF4A4A55) else lerp(Accent, Color.White, 0.42f)
}

data class UiStateColors(val accent: Color)

fun colorsFor(state: ConnectionState): UiStateColors = when (state) {
    ConnectionState.DISCONNECTED -> UiStateColors(XioColors.DisconnectedBlue)
    ConnectionState.CONNECTING -> UiStateColors(XioColors.ConnectingCyan)
    ConnectionState.CONNECTED -> UiStateColors(XioColors.ConnectedGreen)
    ConnectionState.DISCONNECTING -> UiStateColors(XioColors.DisconnectingAmber)
    ConnectionState.ERROR -> UiStateColors(XioColors.ErrorRed)
}

@Composable
fun XioVpnTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    AppearanceSettings.attach(context)

    // Reading the state here makes the whole tree recompose on a theme change.
    val accent = XioColors.Accent
    val background = XioColors.BackgroundTop
    val surface = XioColors.Surface
    val onSurfaceVariant = XioColors.TextSecondary

    val scheme = darkColorScheme(
        primary = accent,
        secondary = XioColors.DisconnectedBlue,
        background = background,
        surface = surface,
        onPrimary = if (accent.luminance() > 0.179f) Color.Black else Color.White,
        onBackground = XioColors.TextPrimary,
        onSurface = XioColors.TextPrimary,
        onSurfaceVariant = onSurfaceVariant,
        primaryContainer = XioColors.AccentMuted,
        onPrimaryContainer = XioColors.TextPrimary,
        surfaceVariant = XioColors.Surface,
        error = XioColors.ErrorRed,
        outline = XioColors.GlassBorder,
    )

    ProvideFixedFontScale(scaled = AppearanceSettings.fontPercent / 100f) {
        MaterialTheme(
            colorScheme = scheme,
            content = content,
        )
    }
}
