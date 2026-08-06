# vdomov-android-app

Native Android WebView App for [www.vdomov.com](https://www.vdomov.com).

## Overview
This application wraps [www.vdomov.com](https://www.vdomov.com) into a full-featured Android application featuring:
- Fullscreen WebView interface with DOM storage & JavaScript enabled
- Page loading progress bar
- Hardware acceleration & back button history navigation
- Automated build pipeline compiled directly on Android/Termux

## Building from Source

To compile the APK from source using JDK 17 & Android AAPT/d8 tools:

```bash
cd NativeAndroidApp
./build_apk.sh
```

The output APK will be generated at `app-release.apk`.
