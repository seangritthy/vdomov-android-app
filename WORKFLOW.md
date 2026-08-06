# VDOmov Multi-Platform Build & Release Workflow Guide

This workspace (`~/NativeAndroidApp`) builds and publishes the **VDOmov** app (`https://www.vdomov.com`) across Mobile, Android TV, Android Tablet, and Windows PC.

---

## 🚀 Quick Commands

To compile all platforms and generate binaries:

```bash
cd ~/NativeAndroidApp
bash build_apk.sh
```

### Generated Artifacts
1. **`app-release.apk`**: Standard Android Release APK
2. **`vdomov-mobile.apk`**: Mobile-optimized Android APK
3. **`vdomov-tv.apk`**: Android TV Leanback D-Pad navigation APK
4. **`vdomov-tablet.apk`**: Android Tablet & Split-Screen APK
5. **`vdomov-pc.exe`**: Windows PC Standalone 1-Click Executable Installer
6. **`vdomov-setup.exe`**: Windows PC Setup executable alias

---

## 📌 Release Publishing Workflow

To publish a new version release across both GitHub repositories (`seangritthy/vdomov-apks` and `seangritthy/vdomov-android-app`):

```bash
# 1. Update version in AndroidManifest.xml (e.g., v1.0.7)
# 2. Build binaries
bash build_apk.sh

# 3. Commit and push source code
git add .
git commit -m "Release v1.0.7 with updates"
git push origin main

# 4. Create public releases on both GitHub repos
gh release create v1.0.7 --repo seangritthy/vdomov-apks app-release.apk vdomov-mobile.apk vdomov-tv.apk vdomov-tablet.apk vdomov-pc.exe vdomov-setup.exe --title "VDOmov v1.0.7" --notes "Release v1.0.7"

gh release create v1.0.7 --repo seangritthy/vdomov-android-app app-release.apk vdomov-mobile.apk vdomov-tv.apk vdomov-tablet.apk vdomov-pc.exe vdomov-setup.exe --title "VDOmov v1.0.7" --notes "Release v1.0.7"
```

---

## 🛡️ Integrated Security & Features
- **OriginGuard AdBlocker**: Dynamic blocklist downloader from StevenBlack hosts (15,000+ domains).
- **Anti-Redirect Shield**: Popup confirmation dialog before opening external redirect links.
- **In-App Auto-Updater**: Automatic GitHub Release version checking against `seangritthy/vdomov-apks` with direct Package Installer targeting to bypass app chooser dialogs.
- **Android TV D-Pad Focus**: Non-touchscreen Leanback launcher support.
- **Windows PC 1-Click Installer**: NSIS compiled installer (`pc_build/vdomov_pc_installer.nsi`).
