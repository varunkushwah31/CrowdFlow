# 📱 CrowdFlow Mobile Client (Flutter / Dart)

Cross-platform mobile application architecture for Indian citizens to report, map, and track community water infrastructure issues.

---

## 🏗️ Architecture & Component Stack

| Layer                    | Library / Tool             | Role in Indian Civic Context                                                           |
|--------------------------|----------------------------|----------------------------------------------------------------------------------------|
| **Framework**            | **Flutter 3.x (Dart 3.x)** | High-performance 60 FPS vector map rendering on Android & iOS.                         |
| **Vector Map Engine**    | `maplibre_gl`              | GPU-accelerated vector map tiles avoiding commercial Google Maps API fees.             |
| **Geolocation**          | `geolocator`               | Captures high-accuracy GPS coordinates at the reporting site.                          |
| **Live Camera**          | `camera` + `image_picker`  | Enforces live on-site camera capture over stale gallery uploads.                       |
| **Hardware EXIF**        | `native_exif`              | Reads raw GPS latitude, longitude, altitude, and timestamp directly from image stream. |
| **Offline Cache**        | `drift` (SQLite)           | Stores reports locally if a citizen reports in areas with zero cellular connectivity.  |
| **Resilient Networking** | `dio`                      | Handles chunked multipart HTTP uploads with background retries.                        |

---

## ⚡ Setup & Execution

### Prerequisites
- Flutter SDK 3.16+
- Android Studio / Xcode

### Install Dependencies
```bash
flutter pub get
```

### Run Application
```bash
flutter run
```

---

## 📡 Backend Connectivity
- **Android Emulator**: Connects to host via `http://10.0.2.2:8085`
- **iOS Simulator**: Connects to host via `http://localhost:8085`
- **Physical Device**: Connect to your workstation's local LAN IP: `http://192.168.x.x:8085`
