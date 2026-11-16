package com.xiovpn.mobile.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

internal object AppFontScale {
    /** Android's own font-scale setting is ignored; the app owns the scale. */
    const val FIXED = 1f

    fun wrap(base: Context): Context {
        val current = base.resources.configuration.fontScale
        if (current == FIXED) return base
        val config = Configuration(base.resources.configuration)
        config.fontScale = FIXED
        return base.createConfigurationContext(config)
    }

    fun lock(configuration: Configuration): Configuration {
        if (configuration.fontScale == FIXED) return configuration
        return Configuration(configuration).apply { fontScale = FIXED }
    }

    fun lock(resources: Resources) {
        val config = resources.configuration
        if (config.fontScale == FIXED) return
        config.fontScale = FIXED
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

/**
 * Pins the type scale to [scaled] times the baseline, ignoring the system font
 * size. The in-app appearance setting drives [scaled] so text keeps the size
 * the user chose for XioVPN itself.
 */
@Composable
internal fun ProvideFixedFontScale(
    scaled: Float = AppFontScale.FIXED,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val value = scaled.coerceIn(0.8f, 1.4f)
    val fixed = remember(density.density, value) {
        Density(density.density, fontScale = value)
    }
    CompositionLocalProvider(LocalDensity provides fixed, content = content)
}
