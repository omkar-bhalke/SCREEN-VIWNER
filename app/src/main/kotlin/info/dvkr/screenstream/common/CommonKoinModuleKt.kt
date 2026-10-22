package info.dvkr.screenstream.common

import info.dvkr.screenstream.common.analytics.StreamingAnalytics
import info.dvkr.screenstream.common.analytics.StreamingAnalyticsImpl
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.common.module.StreamingModuleManager
import info.dvkr.screenstream.common.settings.AppSettings
import info.dvkr.screenstream.common.settings.AppSettingsImpl
import org.koin.dsl.module

object CommonKoinModuleKt {
    val CommonKoinModule = module {
        single<AppSettings> { AppSettingsImpl(androidContext()) }
        single<StreamingAnalytics> { StreamingAnalyticsImpl() }
        // StreamingModuleManager is created after all module Koin modules load
        // It receives the list via lazy injection in BaseApp
    }
}
