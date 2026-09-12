# Host Finder Pro — Android

**Your Entire Attack Surface. On mobile.**

Android client for [Host Finder Pro Enterprise](https://github.com/yosefbatru-cmd/host-finder-pro).

## Features

- Connect to Host Finder Pro API (configurable base URL)
- Quick scan — add domain + trigger full recon
- Domain list + rescan
- Scan job progress with live refresh
- Live subdomain enumeration
- Dark enterprise UI (Jetpack Compose + Material 3)

## Build APK (GitHub Actions)

1. Open **Actions** → **Build APK** → **Run workflow**
2. Download artifact **HostFinderPro-debug**

Or push to `main` — CI builds automatically.

## Local build

```bash
# Need JDK 17 + Android SDK
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## API URL

| Environment | Base URL |
|-------------|----------|
| Android emulator → host machine | `http://10.0.2.2:8000` |
| Physical device (same LAN) | `http://<your-pc-ip>:8000` |
| Deployed backend | `https://your-api.example.com` |

Set it under the **API** tab in the app.

## Pair with backend

```bash
git clone https://github.com/yosefbatru-cmd/host-finder-pro.git
cd host-finder-pro/backend
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## Package

`com.hostfinder.pro` · minSdk 26 · targetSdk 35
