package info.dvkr.screenstream.webrtc

import info.dvkr.screenstream.webrtc.internal.WebRtcEnvironment
import info.dvkr.screenstream.webrtc.internal.WebRtcStreamingService
import info.dvkr.screenstream.webrtc.settings.WebRtcSettings
import info.dvkr.screenstream.webrtc.ui.WebRtcViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val WebRtcKoinModule = module {
    single { WebRtcSettings.create(androidContext()) }
    single { WebRtcEnvironment(androidContext()) }
    single { WebRtcStreamingService(androidContext(), get(), get()) }
    viewModel { WebRtcViewModel(get()) }
}
