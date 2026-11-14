package com.xiovpn.mobile.ai

import com.xiovpn.mobile.mci.MciEdge
import com.xiovpn.mobile.profiles.ProxyProfile

/**
 * A second exit used only by AI domains.
 *
 * The exit is one of the user's own profiles, reached over the same Cloudflare edge and the
 * same transport the main tunnel already proved to work. Nothing here is user supplied and
 * nothing here changes the main tunnel.
 */
data class AiRoutePlan(
    val profile: ProxyProfile,
    val edge: MciEdge,
    val domains: List<String> = AiRouteDomains.RULES,
) {
    val isUsable: Boolean
        get() = domains.isNotEmpty() && profile.id.isNotBlank()
}
