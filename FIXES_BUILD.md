# Perbaikan build Laper Intro

Perbaikan yang diterapkan berdasarkan log GitHub Actions:

1. Minecraft 26.3 menggabungkan block berwarna ke koleksi `ColorCollection`; gunakan `Blocks.CONCRETE.pick(DyeColor.WHITE)` dan `Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE)`.
2. API Entity 26.3 menggunakan `setPermanentlyInvulnerable(boolean)`; status permanen yang disimpan dipulihkan melalui API yang sama.
3. Player Animation Library dipindahkan dari `clientImplementation` ke `implementation` agar interface yang disisipkan ke `Avatar` tersedia ketika source set `main` dikompilasi. Metadata mod PAL tetap `recommends`, karena kode client sudah memeriksa apakah PAL terpasang dan dapat berjalan tanpa animasi jika tidak ada.

Catatan: arsip ini sudah diperiksa secara statis, tetapi build Gradle belum dapat dijalankan di lingkungan ini karena Java yang tersedia adalah 21, sedangkan proyek memerlukan Java 25. Jalankan `./gradlew build` melalui GitHub Actions untuk verifikasi kompilasi akhir.
