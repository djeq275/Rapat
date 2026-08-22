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

## 2. User Experience & Functionality

### User Personas

| Persona | Peran |
|---|---|
| **Admin** | Mengelola pengguna, divisi, dan penunjukan notulis lintas divisi. |
| **Direktur** | Pemangku masalah utama: butuh visibilitas penuh atas semua rapat lintas divisi dan akses cepat ke notulensi. |
| **Ketua Divisi** | Membuat & mengelola jadwal rapat divisinya, menunjuk notulis, mengisi notulensi. |
| **Karyawan** | Peserta rapat; melihat jadwal rapat divisinya dan notulensi rapat yang bisa diaksesnya; bisa ditunjuk sebagai notulis untuk rapat tertentu. |

### User Stories & Acceptance Criteria

**US-1 — Login**
Sebagai pengguna, saya ingin login menggunakan email/password atau akun Google, supaya saya bisa masuk dengan cara yang paling nyaman.
- AC: Login berhasil dengan kombinasi email+password yang valid.
- AC: Login berhasil via OAuth Google, dan akun Google yang baru pertama kali login otomatis tertaut ke akun pengguna yang sudah ada berdasarkan email.
- AC: Login akun Google adalah prasyarat untuk fitur sync Google Calendar (izin/scope calendar diminta saat itu).

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

### Non-Goals

- Tidak membangun aplikasi mobile native (web responsive melalui Thymeleaf sudah cukup untuk rilis ini).
- Tidak mendukung multi-perusahaan/multi-tenant — aplikasi ini untuk satu perusahaan.
- Tidak mendukung rapat berulang (*recurring meeting*) pada rilis ini.
- Tidak membangun integrasi video conference (Zoom/Google Meet) — cukup link materi/notulensi berupa Google Doc.
- Tidak menyalin/menduplikasi konten Google Doc ke database — aplikasi hanya menyimpan link + ringkasan notulensi berupa teks, bukan mem-*mirror* dokumen.
- Tidak membangun dashboard analitik/rekap kehadiran rapat pada rilis ini.

## 3. Technical Specifications

**Bukan sistem AI** — bagian "AI System Requirements" tidak berlaku untuk PRD ini.

### Architecture Overview

- **Spring Boot MVC + Thymeleaf**: aplikasi web server-rendered, satu modular monolith.
- **Spring Modulith**: setiap domain (mis. `division`, `meeting`, `user`, `notulensi`) menjadi modul terpisah di bawah `com.example.vibe1.*`, dengan batas modul yang diverifikasi Modulith.
- **Alur utama pembuatan rapat**: Ketua Divisi mengisi form rapat → aplikasi menyimpan entitas `Meeting` beserta daftar peserta (termasuk Direktur yang ditambahkan otomatis) → aplikasi memanggil Google Calendar API untuk membuat event dengan attendee list → status sync disimpan di `Meeting` (sukses/gagal + timestamp).
- **Alur notulensi**: Ketua Divisi/Admin menunjuk notulis untuk rapat tertentu → hak edit tersimpan sebagai relasi (mis. tabel penunjukan notulis per-rapat) → notulis/Ketua Divisi mengisi link Google Doc + ringkasan notulensi pada detail rapat → peserta rapat (sesuai aturan visibilitas divisi) dapat membaca hasilnya.

### Integration Points

- **Google OAuth2** (Spring Security OAuth2 Client) untuk login akun Google dan memperoleh consent scope Google Calendar.
- **Google Calendar API** untuk membuat/memperbarui/membatalkan event rapat beserta attendee.
- **MariaDB** melalui **Spring Data JPA**, skema dikelola dengan **Liquibase** (changelog per fitur, tidak ada perubahan skema manual di luar migration).

### Security & Privacy

- **RBAC** berbasis peran (Admin, Direktur, Ketua Divisi, Karyawan) untuk kontrol akses fitur secara umum (siapa bisa membuat rapat, siapa bisa melihat divisi mana).
- **Izin granular per-rapat** untuk penunjukan notulis — dimodelkan sebagai relasi terpisah dari role, agar tidak mencemari hierarki RBAC dengan role sementara/per-kasus.
- **Token OAuth Google** (access/refresh token) disimpan terenkripsi di database, tidak pernah di-log atau ditampilkan di UI.
- Kredensial database & OAuth client disimpan sebagai environment variable (`.env`, tidak masuk version control), bukan hardcoded di source atau file scope.
- Data yang disimpan aplikasi terbatas pada metadata rapat (judul, waktu, peserta, link, ringkasan notulensi) — dokumen Google Doc sendiri tetap berada di Google Workspace perusahaan, tunduk pada kontrol akses Google Workspace yang sudah ada.

## 4. Risks & Roadmap

### Rollout

Rilis dilakukan **sekaligus dengan scope penuh** (US-1 s/d US-5 di atas) — tidak dipecah menjadi MVP/v1.1 pada PRD ini. Item di bagian Non-Goals (mobile app, multi-tenant, recurring meeting, integrasi video conference, dashboard analitik) menjadi kandidat backlog untuk evaluasi kebutuhan berikutnya, bukan komitmen rilis ini.

### Technical Risks

- **Ketergantungan pada Google Calendar API**: kuota/rate limit, atau perubahan token OAuth (expired/revoked) dapat menyebabkan sync gagal — perlu mekanisme retry & indikator status sync yang terlihat oleh Ketua Divisi.
- **Consent OAuth per pengguna**: setiap pengguna (termasuk Direktur) harus memberi izin akses Google Calendar; adopsi bisa terhambat jika proses consent tidak jelas.
- **Reliabilitas notifikasi tetap bergantung pada sistem Google Calendar** di sisi klien (pengaturan notifikasi akun Google masing-masing pengguna) — aplikasi hanya memastikan event & invite terkirim dengan benar, bukan menjamin pengaturan notifikasi pribadi pengguna.
- **Migrasi skema via Liquibase**: perubahan skema untuk entitas baru (rapat, penunjukan notulis, dsb.) harus terkontrol lewat changelog agar tidak konflik antar developer.
- **Kesalahan pemodelan RBAC vs izin per-rapat**: risiko implementasi menyederhanakan penunjukan notulis menjadi role global, yang akan sulit di-scale dan tidak sesuai kebutuhan (lihat US-5).
