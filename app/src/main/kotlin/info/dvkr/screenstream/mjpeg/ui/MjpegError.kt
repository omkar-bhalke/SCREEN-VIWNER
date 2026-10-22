package info.dvkr.screenstream.mjpeg.ui

sealed class MjpegError {
    object CastPermissionsDenied : MjpegError()
    data class AddressInUse(val port: Int) : MjpegError()
    data class Other(val throwable: Throwable) : MjpegError()

    companion object {
        fun from(throwable: Throwable): MjpegError = when {
            throwable is java.net.BindException -> AddressInUse(8080)
            else -> Other(throwable)
        }
    }
}
