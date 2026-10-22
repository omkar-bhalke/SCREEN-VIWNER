package info.dvkr.screenstream.rtsp

import info.dvkr.screenstream.rtsp.internal.RtspStreamingService
import info.dvkr.screenstream.rtsp.settings.RtspSettings
import info.dvkr.screenstream.rtsp.ui.RtspViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val RtspKoinModule = module {
    single { RtspSettings.create(androidContext()) }
    single { RtspStreamingService(androidContext(), get()) }
    viewModel { RtspViewModel(get()) }
}
