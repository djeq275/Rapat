# PRD — Aplikasi Penjadwalan & Notulensi Rapat Internal

## 1. Executive Summary

**Problem Statement**
Direktur sering terlewat rapat yang diadakan oleh divisi-divisi di bawahnya karena tidak ada notifikasi Google Calendar yang berfungsi untuk jadwal rapat yang akan/sedang berlangsung, dan tidak bisa membaca notulensi rapat karena tidak tahu di mana link Google Doc notulensinya disimpan.

**Proposed Solution**
Aplikasi internal berbasis Spring Boot (MVC + Thymeleaf) yang memusatkan penjadwalan rapat per divisi, secara otomatis menambahkan Direktur sebagai peserta di setiap rapat divisi, menyinkronkan rapat sebagai event Google Calendar dengan invite proper (agar notifikasi native Google Calendar berfungsi), dan menyediakan satu tempat untuk mencatat serta mengakses link notulensi & materi rapat.

**Success Criteria (KPI)**
1. **Zero rapat terlewat karena tidak tahu jadwal** — dalam 30 hari setelah rollout, tidak ada laporan Direktur terlewat rapat akibat tidak mendapat notifikasi/tidak tahu jadwal (dibanding kondisi sebelumnya yang rutin terjadi).
2. **Keberhasilan sync kalender ≥ 99%** dari seluruh rapat yang dijadwalkan berhasil terbuat sebagai event Google Calendar dengan invite terkirim ke seluruh peserta tanpa error.
3. **Kelengkapan notulensi ≥ 95%** dari rapat yang sudah selesai memiliki link/notulensi tercatat dan dapat diakses oleh peserta terkait dalam 3 hari kerja setelah rapat selesai.
4. **Adopsi 100%** — seluruh Ketua Divisi menjadwalkan rapat melalui aplikasi (bukan lagi manual/WA/kalender pribadi) dalam 1 bulan setelah rollout.
5. **Notifikasi Telegram terkirim ≥ 95%** dari rapat yang memilih minimal satu grup Telegram tujuan berhasil mengirim pesan undangan ke seluruh grup terpilih tanpa error.

## 2. User Experience & Functionality

### User Personas

| Persona | Peran |
|---|---|
| **Admin** | Mengelola pengguna, divisi, penunjukan notulis lintas divisi, dan data grup Telegram. |
| **Direktur** | Pemangku masalah utama: butuh visibilitas penuh atas semua rapat lintas divisi dan akses cepat ke notulensi. |
| **Ketua Divisi** | Membuat & mengelola jadwal rapat divisinya, memilih grup Telegram tujuan undangan, menunjuk notulis, mengisi notulensi. |
| **Karyawan** | Peserta rapat; melihat jadwal rapat divisinya dan notulensi rapat yang bisa diaksesnya; bisa ditunjuk sebagai notulis untuk rapat tertentu. |

### User Stories & Acceptance Criteria

**US-1 — Login**
Sebagai pengguna, saya ingin login menggunakan email/password, akun Google, atau Single Sign-On (SSO) Keycloak kantor, supaya saya bisa masuk dengan cara yang paling nyaman/sesuai kebijakan kantor.
- AC: Login berhasil dengan kombinasi email+password yang valid.
- AC: Login berhasil via OAuth Google, dan akun Google yang baru pertama kali login otomatis tertaut ke akun pengguna yang sudah ada berdasarkan email.
- AC: Login akun Google adalah prasyarat untuk fitur sync Google Calendar (izin/scope calendar diminta saat itu).
- AC: Login berhasil via SSO Keycloak kantor. Jika email dari Keycloak **belum pernah terdaftar**, sistem otomatis membuat akun baru (*auto-provisioning*) dengan role default **Karyawan** dan divisi masih kosong — berbeda dari login Google/email-password yang mengharuskan akun sudah dibuat lebih dulu oleh Admin (lihat Security & Privacy untuk kenapa ini pengecualian, bukan perubahan kebijakan umum).
- AC: Percobaan login Keycloak berikutnya dari email yang sama memakai akun yang sudah dibuat sebelumnya (match by email, pola sama seperti akun Google), tidak membuat akun duplikat.
- AC: Admin melengkapi role & divisi user hasil auto-provisioning Keycloak lewat halaman kelola pengguna yang **sudah ada** — bukan capability baru yang perlu dibangun terpisah.

**US-2 — Membuat jadwal rapat (Ketua Divisi)**
Sebagai Ketua Divisi, saya ingin membuat jadwal rapat dengan judul, keterangan, link materi, waktu mulai/selesai, dan peserta, supaya rapat tercatat dan seluruh peserta diundang secara resmi.
- AC: Form rapat mewajibkan judul, waktu mulai, waktu selesai (selesai > mulai); keterangan dan link materi opsional.
- AC: Peserta yang dipilih terbatas pada karyawan di divisi Ketua Divisi tersebut.
- AC: Setelah disimpan, rapat otomatis dibuat sebagai event Google Calendar dengan seluruh peserta terundang sebagai *attendee* (bukan sekadar entri kalender pribadi pembuat), sehingga notifikasi native Google Calendar (email/popup) aktif untuk semua peserta.
- AC: Jika pembuatan event Google Calendar gagal (misal token OAuth expired), rapat tetap tersimpan di aplikasi dan status sync ditandai gagal, dengan opsi retry.

**US-3 — Direktur otomatis diundang**
Sebagai Direktur, saya ingin otomatis menjadi peserta di setiap rapat yang dibuat divisi mana pun, supaya saya tidak pernah terlewat tanpa harus diundang manual oleh setiap Ketua Divisi.
- AC: Setiap rapat yang dibuat oleh Ketua Divisi manapun otomatis memasukkan Direktur ke daftar peserta, tanpa aksi tambahan dari Ketua Divisi.
- AC: Direktur menerima invite Google Calendar untuk rapat tersebut sama seperti peserta lain.
- AC: Direktur bisa melihat rapat ini di daftar rapatnya tanpa perlu tahu divisi mana yang membuatnya.

**US-4 — Melihat daftar & detail rapat**
Sebagai Karyawan, saya ingin melihat daftar rapat divisi saya dan detailnya (termasuk notulensi), supaya saya tahu jadwal dan hasil rapat tanpa harus mencari-cari link secara manual.
- AC: Karyawan hanya melihat daftar rapat pada divisinya sendiri (atau rapat lain tempat ia menjadi peserta terundang).
- AC: Direktur dan Admin melihat daftar rapat dari **semua** divisi.
- AC: Detail rapat menampilkan link Google Doc materi/notulensi dan isi notulensi (jika sudah diisi), dapat diakses oleh siapa pun yang berhak melihat rapat tersebut.

**US-5 — Menunjuk notulis & mengisi notulensi**
Sebagai Ketua Divisi atau Admin, saya ingin menunjuk karyawan tertentu sebagai notulis untuk sebuah rapat, supaya pengisian notulensi tidak harus selalu saya lakukan sendiri.
- AC: Ketua Divisi/Admin dapat menunjuk satu atau lebih karyawan peserta rapat sebagai notulis untuk rapat spesifik tersebut.
- AC: Hak menulis notulensi & menambah link Google Doc berlaku hanya untuk rapat yang ditunjuk, bukan hak global di seluruh rapat (izin per-rapat, bukan role RBAC baru).
- AC: Ketua Divisi tetap memiliki hak menulis notulensi untuk semua rapat divisinya sendiri, tanpa perlu ditunjuk.
- AC: Karyawan yang bukan notulis dan bukan Ketua Divisi/Admin hanya bisa membaca, tidak bisa mengedit notulensi.

**US-6 — Memilih grup Telegram tujuan saat membuat rapat**
Sebagai Ketua Divisi, saya ingin memilih satu atau lebih grup Telegram saat membuat rapat, supaya undangan rapat otomatis terkirim ke grup Telegram yang relevan tanpa perlu forward manual.
- AC: Form pembuatan rapat menampilkan daftar grup Telegram aktif untuk dipilih (bisa lebih dari satu).
- AC: Grup Telegram favorit milik divisi Ketua Divisi tersebut otomatis sudah tercentang saat form dibuka, tapi tetap bisa ditambah/dikurangi sebelum submit.
- AC: Setelah rapat disimpan, sistem otomatis mengirim pesan undangan (judul, waktu, link materi) ke setiap grup Telegram yang dipilih.
- AC: Kegagalan pengiriman ke satu/lebih grup Telegram tidak menggagalkan pembuatan rapat itu sendiri; status kirim per grup dicatat (sukses/gagal), mengikuti pola status sync Google Calendar yang sudah ada (lihat US-2).

**US-7 — Admin mengelola data grup Telegram**
Sebagai Admin, saya ingin menambah, mengubah, dan menghapus data grup Telegram, supaya daftar grup yang tersedia untuk dipilih saat membuat rapat selalu akurat.
- AC: Admin bisa menambah grup Telegram baru dengan nama dan Chat ID.
- AC: Admin bisa mengubah nama, Chat ID, atau status aktif grup Telegram yang sudah ada.
- AC: Admin bisa menonaktifkan/menghapus grup Telegram dari daftar pilihan; rapat lama yang sudah terlanjur mengirim ke grup tersebut tetap menyimpan riwayatnya.

**US-8 — Grup Telegram favorit per divisi**
Sebagai Admin atau Ketua Divisi, saya ingin menandai satu atau lebih grup Telegram sebagai favorit untuk suatu divisi, supaya grup tersebut otomatis terpilih setiap kali ada rapat baru di divisi itu.
- AC: Satu divisi bisa punya lebih dari satu grup Telegram favorit.
- AC: Satu grup Telegram yang sama boleh menjadi favorit lebih dari satu divisi.
- AC: Perubahan daftar favorit divisi hanya memengaruhi default pilihan di form rapat baru berikutnya, tidak mengubah rapat yang sudah dibuat sebelumnya.

**US-9 — Admin mengelola konfigurasi koneksi Keycloak**
Sebagai Admin, saya ingin mengatur koneksi ke Keycloak server (URL server/realm, client ID, client secret) lewat UI aplikasi, supaya saya bisa mengubah konfigurasi tanpa akses ke server/file `.env`.
- AC: Admin bisa mengisi/mengubah issuer/URL server Keycloak, realm, client ID, dan client secret lewat halaman pengaturan.
- AC: Client secret tidak pernah ditampilkan ulang setelah disimpan (pola sama seperti pengaturan token bot Telegram); mengosongkan field client secret saat edit berarti tidak mengubah secret yang sudah tersimpan.
- AC: Perubahan konfigurasi berlaku efektif pada percobaan login berikutnya, tanpa perlu restart aplikasi.
- AC: Tombol "Login dengan Keycloak" di halaman login hanya muncul jika Admin sudah pernah menyimpan konfigurasi; sebelum itu, opsi ini disembunyikan (bukan ditampilkan lalu gagal saat diklik).

**US-10 — Admin mengelola konfigurasi koneksi Google OAuth**
Sebagai Admin, saya ingin mengatur client ID dan client secret Google OAuth lewat UI aplikasi, supaya saya bisa mengubah kredensial tanpa akses ke server/file `.env` dan tanpa restart aplikasi.
- AC: Admin bisa mengisi/mengubah client ID dan client secret Google OAuth lewat halaman pengaturan — satu pasang kredensial dipakai bersama oleh login Google dan consent Google Calendar (sama seperti sekarang, satu aplikasi OAuth di Google Cloud Console).
- AC: Client secret tidak pernah ditampilkan ulang setelah disimpan (pola sama seperti pengaturan Keycloak/token bot Telegram); mengosongkan field client secret saat edit berarti tidak mengubah secret yang sudah tersimpan.
- AC: Perubahan konfigurasi berlaku efektif pada percobaan login/consent Calendar berikutnya, tanpa perlu restart aplikasi.

### Non-Goals

- Tidak membangun aplikasi mobile native (web responsive melalui Thymeleaf sudah cukup untuk rilis ini).
- Tidak mendukung multi-perusahaan/multi-tenant — aplikasi ini untuk satu perusahaan.
- Tidak mendukung rapat berulang (*recurring meeting*) pada rilis ini.
- Tidak membangun integrasi video conference (Zoom/Google Meet) — cukup link materi/notulensi berupa Google Doc.
- Tidak menyalin/menduplikasi konten Google Doc ke database — aplikasi hanya menyimpan link + ringkasan notulensi berupa teks, bukan mem-*mirror* dokumen.
- Tidak membangun dashboard analitik/rekap kehadiran rapat pada rilis ini.
- Notifikasi Telegram bersifat tambahan/paralel terhadap sync Google Calendar — bukan pengganti, dan tidak menggantikan undangan Calendar yang sudah ada.
- Tidak ada percakapan dua arah lewat bot Telegram (bot hanya mengirim pesan satu arah, tidak membaca/merespons pesan dari grup).
- Tidak mendukung platform chat lain (WhatsApp, Slack, dll.) di luar Telegram pada rilis ini.
- Login SSO Keycloak **tidak menghapus atau menggantikan** login Google maupun email/password pada rilis ini — ketiganya berdampingan; auto-provisioning akun hanya berlaku untuk jalur Keycloak.

## 3. Technical Specifications

**Bukan sistem AI** — bagian "AI System Requirements" tidak berlaku untuk PRD ini.

### Architecture Overview

- **Spring Boot MVC + Thymeleaf**: aplikasi web server-rendered, satu modular monolith.
- **Spring Modulith**: setiap domain (mis. `division`, `meeting`, `user`, `notulensi`, `telegram`) menjadi modul terpisah di bawah `com.example.vibe1.*`, dengan batas modul yang diverifikasi Modulith.
- **Alur utama pembuatan rapat**: Ketua Divisi mengisi form rapat (termasuk pilihan grup Telegram tujuan, pre-filled dari favorit divisi) → aplikasi menyimpan entitas `Meeting` beserta daftar peserta (termasuk Direktur yang ditambahkan otomatis) dan daftar grup Telegram terpilih → aplikasi memanggil Google Calendar API untuk membuat event dengan attendee list → status sync disimpan di `Meeting` (sukses/gagal + timestamp).
- **Alur notifikasi Telegram**: berjalan paralel dengan alur sync Google Calendar, dipicu oleh event yang sama saat rapat tersimpan → untuk setiap grup Telegram yang dipilih, aplikasi mengirim pesan undangan lewat Telegram Bot API → status kirim per grup dicatat (sukses/gagal); kegagalan tidak membatalkan pembuatan rapat maupun sync Calendar-nya.
- **Alur notulensi**: Ketua Divisi/Admin menunjuk notulis untuk rapat tertentu → hak edit tersimpan sebagai relasi (mis. tabel penunjukan notulis per-rapat) → notulis/Ketua Divisi mengisi link Google Doc + ringkasan notulensi pada detail rapat → peserta rapat (sesuai aturan visibilitas divisi) dapat membaca hasilnya.
- **Data grup Telegram**: entitas grup Telegram (nama, Chat ID, status aktif) dikelola Admin; relasi favorit menghubungkan satu divisi ke banyak grup (many-to-many) sebagai default pilihan; relasi terpisah mencatat grup mana saja yang benar-benar dipilih untuk satu rapat tertentu (independen dari daftar favorit, karena favorit bisa berubah setelah rapat dibuat).
- **Alur login Keycloak (SSO)**: pengguna memilih "Login dengan Keycloak" → diarahkan ke server Keycloak kantor untuk autentikasi → setelah sukses, aplikasi mencari `User` lokal berdasarkan email dari Keycloak; jika belum ada, aplikasi otomatis membuat `User` baru dengan role `KARYAWAN` dan divisi kosong (auto-provisioning — satu-satunya jalur login yang boleh membuat akun otomatis); jika sudah ada, akun yang sama langsung dipakai. Admin melengkapi divisi & role user tersebut lewat halaman kelola pengguna yang sudah ada.
- **Konfigurasi Keycloak**: disimpan sebagai satu baris config (mirip `TelegramBotConfig`) berisi issuer/URL server, realm, client ID, dan client secret terenkripsi — dikelola Admin lewat UI, bukan `.env` (satu konfigurasi per instalasi aplikasi, tidak mendukung multi-realm/multi-server). Registrasi OAuth2/OIDC untuk `keycloak` dilayani oleh `ClientRegistrationRepository` custom yang membaca config ini dari database dan meng-cache-nya di memory, di-invalidate persis saat Admin menyimpan config baru — supaya perubahan berlaku tanpa restart aplikasi. Registrasi `google-login`/`google-calendar` mengikuti pola dinamis yang sama — lihat bullet konfigurasi Google OAuth di bawah.
- **Konfigurasi Google OAuth**: client ID & client secret (satu pasang, dipakai bersama oleh `google-login` dan `google-calendar` — sama seperti sekarang, satu aplikasi OAuth di Google Cloud Console) disimpan di database, client secret terenkripsi, dikelola Admin lewat UI — menggantikan `GOOGLE_OAUTH_CLIENT_ID`/`GOOGLE_OAUTH_CLIENT_SECRET` di `.env`. Kedua registration id ini dilayani lewat `ClientRegistrationRepository` custom yang sama seperti Keycloak (cache in-memory, di-invalidate saat config disimpan, berlaku tanpa restart). Redirect URI dan scope masing-masing registrasi tetap seperti sekarang (bukan bagian yang diedit Admin) — hanya kredensialnya yang berpindah dari `.env` ke database.

### Integration Points

- **Google OAuth2** (Spring Security OAuth2 Client) untuk login akun Google dan memperoleh consent scope Google Calendar — kredensial (client ID/secret) dikonfigurasi Admin lewat UI aplikasi (bukan `.env`), lihat Architecture Overview.
- **Google Calendar API** untuk membuat/memperbarui/membatalkan event rapat beserta attendee.
- **Keycloak (OpenID Connect)** sebagai identity provider tambahan untuk login SSO karyawan, berdampingan dengan Google OAuth2 login dan email/password — satu-satunya jalur login yang melakukan auto-provisioning akun baru (role default Karyawan) saat email belum terdaftar. Koneksi ke server Keycloak dikonfigurasi Admin lewat UI aplikasi (bukan `.env`), lihat Architecture Overview.
- **Telegram Bot API** untuk mengirim pesan undangan rapat ke grup Telegram yang dipilih. Bot harus sudah ditambahkan sebagai member/admin di grup tujuan sebelum bisa mengirim pesan — prasyarat operasional manual, bukan sesuatu yang bisa diotomatisasi dari sisi aplikasi.
- **MariaDB** melalui **Spring Data JPA**, skema dikelola dengan **Liquibase** (changelog per fitur, tidak ada perubahan skema manual di luar migration).

### Security & Privacy

- **RBAC** berbasis peran (Admin, Direktur, Ketua Divisi, Karyawan) untuk kontrol akses fitur secara umum (siapa bisa membuat rapat, siapa bisa melihat divisi mana).
- **Izin granular per-rapat** untuk penunjukan notulis — dimodelkan sebagai relasi terpisah dari role, agar tidak mencemari hierarki RBAC dengan role sementara/per-kasus.
- **Token OAuth Google** (access/refresh token per pengguna) disimpan terenkripsi di database, tidak pernah di-log atau ditampilkan di UI.
- Kredensial database disimpan sebagai environment variable (`.env`, tidak masuk version control). Kredensial OAuth client (Google, Keycloak) dan token bot Telegram **tidak** lagi berupa environment variable — disimpan di database, terenkripsi, dikelola Admin lewat UI; hanya key enkripsinya sendiri yang tetap di `.env`.
- Data yang disimpan aplikasi terbatas pada metadata rapat (judul, waktu, peserta, link, ringkasan notulensi) — dokumen Google Doc sendiri tetap berada di Google Workspace perusahaan, tunduk pada kontrol akses Google Workspace yang sudah ada.
- **Auto-provisioning akun khusus login Keycloak**: satu-satunya pengecualian terhadap kebijakan "tidak ada self-registration" di aplikasi ini — login Google dan email/password tetap mengharuskan akun sudah dibuat manual oleh Admin lebih dulu, tidak berubah oleh hadirnya Keycloak SSO.
- **Client secret Keycloak dan client secret Google OAuth** masing-masing disimpan terenkripsi di database dengan key enkripsi sendiri-sendiri, terpisah dari key Telegram (supaya kompromi satu key tidak membuka secret lain, pola sama seperti `AesGcmStringConverter` yang sudah ada) — tidak pernah di-log atau ditampilkan ulang di UI.

## 4. Risks & Roadmap

### Rollout

Rilis awal dilakukan **sekaligus dengan scope penuh** (US-1 s/d US-5) — tidak dipecah menjadi MVP/v1.1. Notifikasi Telegram (US-6 s/d US-8) adalah **penambahan requirement setelah rilis awal**, mengikuti pola rilis yang sama (langsung scope penuh, bukan bertahap). Item di bagian Non-Goals (mobile app, multi-tenant, recurring meeting, integrasi video conference, dashboard analitik, platform chat selain Telegram) menjadi kandidat backlog untuk evaluasi kebutuhan berikutnya, bukan komitmen rilis ini.

### Technical Risks

- **Ketergantungan pada Google Calendar API**: kuota/rate limit, atau perubahan token OAuth (expired/revoked) dapat menyebabkan sync gagal — perlu mekanisme retry & indikator status sync yang terlihat oleh Ketua Divisi.
- **Consent OAuth per pengguna**: setiap pengguna (termasuk Direktur) harus memberi izin akses Google Calendar; adopsi bisa terhambat jika proses consent tidak jelas.
- **Reliabilitas notifikasi tetap bergantung pada sistem Google Calendar** di sisi klien (pengaturan notifikasi akun Google masing-masing pengguna) — aplikasi hanya memastikan event & invite terkirim dengan benar, bukan menjamin pengaturan notifikasi pribadi pengguna.
- **Migrasi skema via Liquibase**: perubahan skema untuk entitas baru (rapat, penunjukan notulis, dsb.) harus terkontrol lewat changelog agar tidak konflik antar developer.
- **Kesalahan pemodelan RBAC vs izin per-rapat**: risiko implementasi menyederhanakan penunjukan notulis menjadi role global, yang akan sulit di-scale dan tidak sesuai kebutuhan (lihat US-5).
- **Prasyarat manual bot Telegram**: bot harus sudah ditambahkan sebagai member/admin di setiap grup tujuan, dan Chat ID grup harus diperoleh & dimasukkan Admin secara manual — tidak ada mekanisme discovery/consent otomatis seperti OAuth Google Calendar, sehingga rawan human error saat setup awal.
- **Rate limit Telegram Bot API**: pengiriman ke banyak grup sekaligus (mis. rapat dengan banyak grup terpilih, atau banyak rapat dibuat bersamaan) berisiko tertunda/gagal jika kena limit.
- **Kegagalan kirim Telegram tidak boleh menggagalkan pembuatan rapat**: harus mengikuti pola yang sama seperti status `FAILED` pada sync Google Calendar (lihat US-2) — gagal kirim ke satu grup tidak boleh membatalkan rapat atau sync Calendar-nya.
- **Akun Karyawan hasil auto-provisioning Keycloak belum punya divisi**: sampai Admin menugaskan divisinya, user tersebut tidak akan melihat rapat apa pun (mengikuti aturan visibilitas per-divisi yang sudah ada) — keterbatasan yang disengaja, bukan bug, tapi perlu dikomunikasikan dengan jelas ke pengguna baru (mis. pesan di halaman beranda) supaya tidak disalahartikan sebagai defect saat rollout.
- **Registrasi OAuth2/OIDC Spring Security default-nya statis** (dibaca sekali saat startup dari `application.properties`) — supaya konfigurasi Keycloak dari database bisa berlaku tanpa restart, dibutuhkan `ClientRegistrationRepository` custom dengan cache in-memory yang di-invalidate saat Admin menyimpan config baru (lihat Architecture Overview); pola ini sudah punya presedan di `RegistrationRoutingAuthorizedClientRepository` yang ada di codebase untuk kasus serupa (routing token per registration id).
- **Migrasi kredensial Google OAuth dari `.env` ke database**: instalasi yang sudah berjalan punya `GOOGLE_OAUTH_CLIENT_ID`/`SECRET` di `.env` — beda dengan Keycloak (fitur opsional, aman kalau belum dikonfigurasi), Google login & Calendar sync adalah fitur inti yang sudah dipakai sejak rilis awal, jadi aplikasi tidak boleh "kehilangan" akses Google begitu perubahan ini di-deploy. Strategi bootstrap (auto-seed baris config dari `.env` saat tabel masih kosong, vs. `.env` tetap jadi fallback permanen) **belum diputuskan** — didorong ke issue desain teknis terpisah saat implementasi.
