package info.dvkr.screenstream.common.analytics

interface StreamingAnalytics {
    fun onStreamStart(moduleId: String)
    fun onStreamStop(moduleId: String, durationMs: Long)
    fun onError(moduleId: String, errorClass: String)
}

class StreamingAnalyticsImpl : StreamingAnalytics {
    override fun onStreamStart(moduleId: String) {}
    override fun onStreamStop(moduleId: String, durationMs: Long) {}
    override fun onError(moduleId: String, errorClass: String) {}
}
