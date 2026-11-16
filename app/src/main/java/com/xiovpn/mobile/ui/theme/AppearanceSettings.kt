package com.xiovpn.mobile.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * One selectable accent, ported from the Dark TV palette.
 * Labels are shown in the appearance screen.
 */
internal data class AccentSwatch(
    val persian: String,
    val english: String,
    val color: Color,
)

/**
 * The five accents from Dark TV, in the same order so a user switching between
 * the two apps lands on a similar colour.
 */
internal val AccentSwatches = listOf(
    AccentSwatch("بنفش", "Violet", Color(0xFF8B5CF6)),
    AccentSwatch("آبی", "Blue", Color(0xFF347EF1)),
    AccentSwatch("فیروزه‌ای", "Cyan", Color(0xFF00B6C9)),
    AccentSwatch("سرخابی", "Pink", Color(0xFFEB1D69)),
    AccentSwatch("نارنجی", "Orange", Color(0xFFFF7037)),
)

/** Surface treatments. Both are dark; XioVPN has no light variant. */
internal object ThemeStyle {
    /** Layered translucent glass over a deep navy gradient. */
    const val LIQUID = "liquid"

    /** Pure-black surfaces with hairline separation, for OLED panels. */
    const val OLED = "oled"

    val all = listOf(LIQUID, OLED)

    fun isKnown(value: String?): Boolean = value in all
}

/**
 * Global appearance state.
 *
 * Deliberately a singleton: [XioColors] is read from ~34 screens and from
 * several top-level `val`s that are initialised outside composition. Backing
 * every colour with Compose snapshot state lets the whole app re-theme from a
 * single write here, without threading a palette parameter through every call
 * site.
 *
 * Reads of [accentIndex], [style] and [fontPercent] must happen inside
 * composition for recomposition to be observed; [XioColors] getters take care
 * of that because Compose tracks the snapshot read even through a plain
 * property accessor.
 */
internal object AppearanceSettings {

    private const val PREFS = "xio_appearance"
    private const val KEY_ACCENT = "accent_index"
    private const val KEY_STYLE = "surface_style"
    private const val KEY_FONT = "font_percent"

    private var prefs: SharedPreferences? = null

    var accentIndex by mutableIntStateOf(0)
        private set

    var style by mutableStateOf(ThemeStyle.LIQUID)
        private set

    var fontPercent by mutableIntStateOf(100)
        private set

    /** Load once, from the application context, before the first composition. */
    @Synchronized
    fun attach(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accentIndex = store.getInt(KEY_ACCENT, 0).coerceIn(0, AccentSwatches.lastIndex)
        style = store.getString(KEY_STYLE, ThemeStyle.LIQUID)
            ?.takeIf(ThemeStyle::isKnown) ?: ThemeStyle.LIQUID
        fontPercent = store.getInt(KEY_FONT, 100).coerceIn(MIN_FONT, MAX_FONT)
        prefs = store
    }

    fun setAccent(index: Int) {
        val next = index.coerceIn(0, AccentSwatches.lastIndex)
        if (next == accentIndex) return
        accentIndex = next
        prefs?.edit()?.putInt(KEY_ACCENT, next)?.apply()
    }

    fun applyStyle(value: String) {
        val next = value.takeIf(ThemeStyle::isKnown) ?: ThemeStyle.LIQUID
        if (next == style) return
        style = next
        prefs?.edit()?.putString(KEY_STYLE, next)?.apply()
    }

    fun applyFontPercent(value: Int) {
        val next = value.coerceIn(MIN_FONT, MAX_FONT)
        if (next == fontPercent) return
        fontPercent = next
        prefs?.edit()?.putInt(KEY_FONT, next)?.apply()
    }

    /** The active accent colour. Safe to read outside composition. */
    val accent: Color
        get() = AccentSwatches[accentIndex.coerceIn(0, AccentSwatches.lastIndex)].color

    const val MIN_FONT = 85
    const val MAX_FONT = 130
}
