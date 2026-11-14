package com.xiovpn.mobile.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The shapes the rim light ("light dance") can draw around the Connect button. */
enum class GlowShape(val key: String) {
    /** Classic lapping comet: a bright head with a fading tail on the rim. */
    COMET("comet"),

    /** The whole ring pulses between two intensities. */
    PULSE("pulse"),

    /** Symmetric twin beams sweeping the rim in opposite directions. */
    TWIN("twin"),

    ;

    companion object {
        fun fromKey(key: String?): GlowShape =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: COMET
    }
}

data class GlowSettings(
    /** Master on/off for the rim light, independent of the motion setting. */
    val enabled: Boolean = true,
    val shape: GlowShape = GlowShape.COMET,
    /** null = follow the connection colour; otherwise a fixed ARGB tint. */
    val colorArgb: Int? = null,
    /** Geometry / speed, common to every shape. */
    val speed: Float = 1f,
    val thickness: Float = 1f,
    val length: Float = 1f,
)

/**
 * Persists the rim-light preferences. Read by [com.xiovpn.mobile.ui.ConnectButton]
 * and edited from the Glow settings screen.
 */
class GlowSettingsStore private constructor(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read())
    val settings: StateFlow<GlowSettings> = mutableSettings.asStateFlow()

    private fun read(): GlowSettings = GlowSettings(
        enabled = prefs.getBoolean(KEY_ENABLED, true),
        shape = GlowShape.fromKey(prefs.getString(KEY_SHAPE, GlowShape.COMET.key)),
        colorArgb = prefs.getInt(KEY_COLOR, NO_COLOR).takeIf { it != NO_COLOR },
        speed = prefs.getFloat(KEY_SPEED, 1f).coerceIn(0.25f, 3f),
        thickness = prefs.getFloat(KEY_THICKNESS, 1f).coerceIn(0.5f, 2.5f),
        length = prefs.getFloat(KEY_LENGTH, 1f).coerceIn(0.3f, 1.8f),
    )

    fun snapshot(): GlowSettings = mutableSettings.value

    private fun persist(next: GlowSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, next.enabled)
            .putString(KEY_SHAPE, next.shape.key)
            .putInt(KEY_COLOR, next.colorArgb ?: NO_COLOR)
            .putFloat(KEY_SPEED, next.speed)
            .putFloat(KEY_THICKNESS, next.thickness)
            .putFloat(KEY_LENGTH, next.length)
            .apply()
        mutableSettings.value = next
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) = persist(snapshot().copy(enabled = enabled))

    @Synchronized
    fun setShape(shape: GlowShape) = persist(snapshot().copy(shape = shape))

    /** [argb] null restores "follow the connection status colour". */
    @Synchronized
    fun setColor(argb: Int?) = persist(snapshot().copy(colorArgb = argb))

    @Synchronized
    fun setSpeed(value: Float) = persist(snapshot().copy(speed = value.coerceIn(0.25f, 3f)))

    @Synchronized
    fun setThickness(value: Float) =
        persist(snapshot().copy(thickness = value.coerceIn(0.5f, 2.5f)))

    @Synchronized
    fun setLength(value: Float) = persist(snapshot().copy(length = value.coerceIn(0.3f, 1.8f)))

    @Synchronized
    fun reset() = persist(GlowSettings())

    companion object {
        private const val PREFS = "xiovpn_glow"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SHAPE = "shape"
        private const val KEY_COLOR = "color_argb"
        private const val KEY_SPEED = "speed"
        private const val KEY_THICKNESS = "thickness"
        private const val KEY_LENGTH = "length"
        private const val NO_COLOR = Int.MIN_VALUE

        @Volatile
        private var instance: GlowSettingsStore? = null

        fun get(context: Context): GlowSettingsStore =
            instance ?: synchronized(this) {
                instance ?: GlowSettingsStore(context).also { instance = it }
            }
    }
}
