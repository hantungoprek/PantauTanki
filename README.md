# PantauTanki v1.0.0

Aplikasi pencatatan BBM dan biaya kendaraan yang offline-first.

## Target
- Minimum Android: 8.0 / API 26
- Target SDK: 35
- Kotlin + Jetpack Compose
- SQLite lokal (tanpa server)

## Fitur v1
- Profil kendaraan
- Kendaraan aktif
- Pencatatan pengisian BBM
- Odometer
- Liter dan harga/liter
- Total biaya otomatis
- Statistik dasar konsumsi dan biaya
- Penghapusan profil kendaraan beserta seluruh riwayat

## Build
Buka folder project di Android Studio versi modern yang mendukung AGP 8.6.1.
Kemudian jalankan:

./gradlew :app:assembleDebug

APK debug berada di:
app/build/outputs/apk/debug/app-debug.apk

Catatan:
Backup/restore dan CSV sudah disiapkan sebagai bagian navigasi v1 dan akan menjadi target implementasi berikutnya.
