package com.xiovpn.mobile.location

import android.content.Context
import com.xiovpn.mobile.engine.EngineMode
import com.xiovpn.mobile.engine.EngineModeStore
import com.xiovpn.mobile.engine.pow.PowEngineStore
import com.xiovpn.mobile.engine.tor.TorEngineStore
import com.xiovpn.mobile.profiles.CountryMetadata
import com.xiovpn.mobile.profiles.ProfileStore
import com.xiovpn.mobile.vpn.ExitIpInfoRepository
import java.util.Locale

internal object GpsSpoofTarget {
    fun countryCode(
        engine: EngineMode,
        profileCountry: String?,
        torExit: String?,
        powExit: String?,
        exitIpCountry: String?,
    ): String? {
        val selected = when {
            engine.isTor -> twoLetter(torExit)
            engine.isPow -> twoLetter(powExit)
            else -> twoLetter(profileCountry)
        }
        return selected ?: twoLetter(exitIpCountry)
    }

    fun fromContext(context: Context): String? {
        val app = context.applicationContext
        val engine = EngineModeStore.get(app).snapshot()
        return countryCode(
            engine = engine,
            profileCountry = ProfileStore(app).selectedProfile().country.countryCode,
            torExit = TorEngineStore.get(app).snapshot().exitCountryCode,
            powExit = PowEngineStore.get(app).snapshot().exitCountryCode,
            exitIpCountry = ExitIpInfoRepository.get(app).state.value.info?.countryCode,
        )
    }

    fun displayName(code: String?, persian: Boolean): String {
        val iso = twoLetter(code) ?: return ""
        val locale = if (persian) Locale("fa") else Locale.ENGLISH
        val localized = Locale("", iso).getDisplayCountry(locale).trim()
        if (localized.isNotEmpty() && !localized.equals(iso, ignoreCase = true)) return localized
        return CountryMetadata.resolve(iso, null).countryName
    }

    private fun twoLetter(raw: String?): String? {
        val value = raw?.trim()?.uppercase(Locale.US).orEmpty()
        return value.takeIf { it.length == 2 && it.all { ch -> ch in 'A'..'Z' } }
    }
}
