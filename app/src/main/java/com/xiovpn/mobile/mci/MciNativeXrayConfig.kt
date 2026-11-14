package com.xiovpn.mobile.mci

import com.xiovpn.mobile.ai.AiRoutePlan
import com.xiovpn.mobile.profiles.ProxyProfile
import com.xiovpn.mobile.settings.AdvancedSettingsData


internal object MciNativeXrayConfig {
    fun build(
        edge: MciEdge = MciConfig.PRIMARY_EDGE,
        settings: AdvancedSettingsData = AdvancedSettingsData.DEFAULT,
        profile: ProxyProfile = ProxyProfile.ADVANCED_SETTINGS_TEMPLATE,
        runtimeOptions: MciXrayRuntimeOptions = MciXrayRuntimeOptions.DEFAULT,
        aiRoute: AiRoutePlan? = null,
    ): String = MciXrayConfigBuilder.build(
        edge,
        settings,
        profile,
        nativeTun = true,
        runtimeOptions = runtimeOptions,
        aiRoute = aiRoute,
    )
}
