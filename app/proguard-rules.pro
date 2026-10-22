# ScreenStream ProGuard rules

# Keep WebRTC classes
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**

# Keep OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Keep Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep data classes (Kotlin serialization)
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Timber
-dontwarn org.jetbrains.annotations.**
