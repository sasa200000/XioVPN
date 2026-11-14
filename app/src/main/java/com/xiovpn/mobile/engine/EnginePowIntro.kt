package com.xiovpn.mobile.engine

/**
 * One-shot after this update: open on XioVPN PoW so users see the third engine.
 * Later launches keep whatever the user last picked.
 */
internal object EnginePowIntro {
    const val FLAG_KEY = "pow_intro_v1"

    fun resolve(stored: EngineMode, introApplied: Boolean): EngineMode =
        if (introApplied) stored else EngineMode.XIO_POW
}
