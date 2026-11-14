package com.xiovpn.mobile

import android.app.Application
import android.content.Context
import com.xiovpn.mobile.engine.EngineModeStore
import com.xiovpn.mobile.logging.CrashReportStore

class XioVpnApplication : Application() {
    override fun attachBaseContext(base: Context) {
        UpgradeReset.run(base)
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        CrashReportStore.install(this)
        EngineModeStore.get(this)
    }
}
