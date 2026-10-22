package info.dvkr.screenstream

import android.app.Application
import info.dvkr.screenstream.common.CommonKoinModuleKt
import info.dvkr.screenstream.common.module.StreamingModule
import info.dvkr.screenstream.common.module.StreamingModuleManager
import info.dvkr.screenstream.common.settings.AppSettings
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

abstract class BaseApp : Application() {

    abstract fun getStreamingModules(): Array<StreamingModule>
    abstract fun configureReleaseLogger(tree: Timber.Tree)

    override fun onCreate() {
        super.onCreate()
        initLogger(BuildConfig.DEBUG)

        val streamingModules = getStreamingModules()

        startKoin {
            androidContext(this@BaseApp)
            modules(CommonKoinModuleKt.CommonKoinModule)

            // Register each streaming module's Koin module
            streamingModules.forEach { m -> modules(m.getKoinModule()) }

            // Register the manager with all streaming modules injected
            modules(module {
                single {
                    StreamingModuleManager(
                        modules = streamingModules.toList(),
                        appSettings = get()
                    )
                }
            })
        }
    }

    private fun initLogger(debug: Boolean) {
        if (debug) {
            Timber.plant(Timber.DebugTree())
        } else {
            configureReleaseLogger(Timber.asTree())
        }
    }
}
