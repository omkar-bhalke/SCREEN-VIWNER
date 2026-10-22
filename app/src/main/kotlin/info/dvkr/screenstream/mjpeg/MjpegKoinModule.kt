package info.dvkr.screenstream.mjpeg

import info.dvkr.screenstream.mjpeg.internal.MjpegStreamingService
import info.dvkr.screenstream.mjpeg.settings.MjpegSettings
import info.dvkr.screenstream.mjpeg.ui.MjpegViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val MjpegKoinModule = module {
    single { MjpegSettings.create(androidContext()) }
    single { MjpegStreamingService(androidContext(), get()) }
    viewModel { MjpegViewModel(get()) }
}
