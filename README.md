# ScreenStream — Reversed Source (4.4.2)

Reconstructed from `ScreenStream_4_4_2_antisplit.apk` via DEX analysis.  
Package: `info.dvkr.screenstream` | minSdk 26 | targetSdk 35 | Kotlin 2.0 | Compose

---

## Project layout

```
app/src/main/
├── AndroidManifest.xml
└── kotlin/info/dvkr/screenstream/
    ├── ScreenStreamApp.kt          # Application — Koin init, Firebase
    ├── BaseApp.kt                  # Abstract app base
    ├── SingleActivity.kt           # Entry point activity
    ├── AppUpdateActivity.kt        # Play In-App Update wrapper
    │
    ├── common/
    │   ├── CommonKoinModuleKt.kt   # Root Koin module
    │   ├── module/
    │   │   ├── StreamingModule.kt          # Interface all 3 modules implement
    │   │   ├── StreamingModuleManager.kt   # Start/stop/switch modules
    │   │   └── StreamingModuleService.kt   # Foreground service base
    │   ├── settings/
    │   │   ├── AppSettings.kt      # Global app prefs interface
    │   │   └── AppSettingsImpl.kt  # DataStore impl
    │   ├── analytics/
    │   │   └── StreamingAnalytics.kt
    │   ├── notification/
    │   │   └── NotificationHelper.kt
    │   └── ui/mediaprojection/
    │       └── MediaProjectionPermission.kt
    │
    ├── mjpeg/                      # MODE 1 — HTTP MJPEG
    │   ├── MjpegStreamingModule.kt
    │   ├── MjpegModuleService.kt
    │   ├── internal/
    │   │   ├── BitmapCapture.kt    # MediaProjection → ImageReader → Bitmap
    │   │   ├── HttpServer.kt       # Ktor/Netty HTTP server (port 8080)
    │   │   ├── HttpServerData.kt   # Client tracking, PIN guard
    │   │   ├── MjpegEvent.kt       # Sealed event bus
    │   │   ├── MjpegStreamingService.kt  # Core pipeline
    │   │   ├── MjpegNetInterface.kt
    │   │   └── NetworkHelper.kt
    │   ├── settings/MjpegSettings.kt
    │   └── ui/
    │       ├── MjpegState.kt / MjpegUiEvent.kt / MjpegError.kt
    │       ├── MjpegViewModel.kt
    │       ├── MjpegMainScreenUI.kt
    │       └── main/cards/
    │           ├── MjpegStreamCard.kt
    │           ├── MjpegAddressCard.kt
    │           └── MjpegClientsCard.kt
    │
    ├── rtsp/                       # MODE 2 — RTSP/RTP
    │   ├── RtspStreamingModule.kt
    │   ├── RtspModuleService.kt
    │   ├── internal/
    │   │   ├── RtspStreamingService.kt   # MediaCodec H.264/H.265 encoder
    │   │   ├── RtspSession.kt            # Per-client RTSP state machine
    │   │   └── RtpPacketizer.kt          # H.264→RTP RFC 6184 (FU-A)
    │   ├── settings/RtspSettings.kt
    │   └── ui/
    │       ├── RtspState.kt / RtspUiEvent.kt
    │       ├── RtspViewModel.kt
    │       └── RtspMainScreenUI.kt
    │
    ├── webrtc/                     # MODE 3 — WebRTC
    │   ├── WebRtcStreamingModule.kt
    │   ├── WebRtcModuleService.kt
    │   ├── internal/
    │   │   ├── WebRtcEnvironment.kt      # Signaling config (screenstream.io)
    │   │   └── WebRtcStreamingService.kt # PeerConnection + Socket.IO
    │   ├── settings/WebRtcSettings.kt
    │   └── ui/
    │       ├── WebRtcState.kt / WebRtcUiEvent.kt
    │       ├── WebRtcViewModel.kt
    │       └── WebRtcMainScreenUI.kt
    │
    ├── tile/TileActionService.kt   # Quick Settings tile
    └── ui/
        ├── ScreenStreamContent.kt  # Root Compose scaffold, module tabs
        └── theme/ScreenStreamTheme.kt
```

---

## Streaming architecture

### MJPEG (port 8080)
```
MediaProjection → ImageReader → Bitmap → JPEG compress → MutableSharedFlow<ByteArray>
                                                                 ↓
                                               Ktor Netty HTTP server
                                               multipart/x-mixed-replace boundary
                                               → browser / VLC / any MJPEG client
```

### RTSP (port 8554)
```
MediaProjection → VirtualDisplay → MediaCodec surface encoder (H.264/H.265)
→ AVCC NAL units → RtpPacketizer (FU-A, RFC 6184)
→ UDP RTP datagrams → each RtspSession (per TCP RTSP client)
```
RTSP handshake: OPTIONS → DESCRIBE (SDP) → SETUP (transport ports) → PLAY → streaming

### WebRTC
```
MediaProjection → ScreenCapturerAndroid → VideoSource → VideoTrack
                                                              ↓
Socket.IO ←→ https://screenstream.io/app/socket (signaling)
              Nonce auth: /app/nonce (Play Integrity token)
              Events: clientConnected → createOffer → sendOffer
                      answer → setRemoteDesc
                      iceCandidate → addIceCandidate
              → RTCPeerConnection per viewer
```

---

## Key constants extracted from APK

| Constant | Value |
|---|---|
| WebRTC signaling URL | `https://screenstream.io` |
| Signaling socket path | `/app/socket` |
| Nonce endpoint | `/app/nonce` |
| Cloud project number | `68285893024` |
| AdMob App ID | `ca-app-pub-3406399667931208~2862159727` |
| MJPEG default port | `8080` |
| RTSP default port | `8554` |
| Stream path | `/screenstream` |

---

## What's stubs / needs real impl

- `R.string.*` / `R.drawable.*` → add your own res files
- `google-services.json` → Firebase project config
- RTSP audio encoder (AudioRecord → AAC MediaCodec) → follow same pattern as video encoder in `RtspStreamingService`
- ONVIF device service endpoint (`/onvif/device_service`)
- `WebRtcStreamingService` — `ScreenCapturerAndroid` import from `io.github.webrtc-sdk:android`
- Settings UI screens (general, image, security tabs) — wired from `MjpegSettings.Data` fields, UI pattern matches existing cards
- `RtspStreamingService._state` accessor on `_state` in ViewModel — expose via method
