# Laper Intro (Fabric 26.3)

Sinematik "Perkenalan Member" berdasarkan video referensi. Member adalah **player asli yang online**
(tidak ada mannequin/bot/NPC). Pose dimainkan pada badan player lewat Player Animation Library.

## Alur
1. Install di server **dan** client: Fabric Loader 0.20.1+, Fabric API 0.165.0+26.3, Player Animation Library 1.2.8+mc.26.3, mod ini.
2. Berdiri di tempat panggung yang diinginkan, jalankan `/laperintro setstage`, lalu `/laperintro build`.
3. Edit `config/laperintro.json` (urutan member, kutipan, pose, siluet "???", daftar `admins`), lalu `/laperintro reload`.
4. Semua member online, jalankan `/laperintro start`. `/laperintro stop` untuk menghentikan.

Command hanya untuk singleplayer, console, atau nama di `admins`.

## Isi sinematik (sesuai video, tanpa audio)
Pembuka judul → tiap member: ledakan debu di pad emas, pose, nama di atas `[NN] Nama`, nameplate kuning
`#NN Nama / LAPER HARDCORE / MEMBER NN OF TT`, subtitle kutipan, kamera berganti 3 bidikan, partikel makin ramai →
siluet "???" untuk member bertanda `mystery` → "DAN YANG TERAKHIR..." + konfeti untuk member terakhir →
penutup kerumunan dengan lampu sorot → fade hitam → semua dikembalikan ke posisi semula.

## Build
`./gradlew build` (Java 25). JAR ada di `build/libs/laper-intro-1.0.0.jar`. GitHub Actions: `.github/workflows/build.yml`.

## Catatan teknis
- Kamera adalah ArmorStand tak terlihat yang digerakkan server; client memakainya sebagai kamera. Itu bukan badan member.
- Peserta dibuat kebal selama sinematik (server hardcore) dan dikembalikan ke status semula.
- Langit galaksi di video berasal dari resource pack/shader, tidak dibuat mod ini.
- Siluet "???" memakai Invisibility + Glowing (outline), bukan siluet hitam penuh.
- Sudut pose adalah perkiraan; sesuaikan di `assets/laperintro/player_animations/*.json`.
- Library: PAL ada di `libs/` (MIT). AzureLib tidak dipakai karena tidak ada entity/blok animasi model dalam video.
