## problem
Saya seorang direktur perusahaan. Saya membawahi beberapa divisi. Setiap divisi dipimpin seorang ketua divisi dan beranggotakan beberapa karyawan. Setiap divisi sering menjadwalkan rapat pertemuan dan mengundang saya. Saya sering terlewat tidak ikut rapat karena tidak hafal dan tidak ada notifikasi dari google calendar mengenai waktu jadwal rapat yang akan dan sedang berlangsung. Saya juga tidak bisa membaca notulensi rapat karena tidak tahu dimana link google doc notulensi rapat disimpan.

## Actor
* Admin
* Direktur
* Ketua Divisi
* Karyawan

## functionlity
* Semua pengguna bisa login menggunakan email dan password atau menggunakan akun google
* Ketua divisi bisa membuat jadwal rapat. Memasukkan judul rapat, keterangan rapat, menambahkan link google doc untuk materi rapat, waktu mulai, waktu selesai, kemudian memilih peserta rapat (karyawan divisinya) yang menerima undangan. Jadwal rapat tersinkron ke google calendar sebagai event dengan invite proper (bukan sekadar entri kalender) supaya notifikasi native Google Calendar (email/popup) berfungsi untuk semua peserta.
* Direktur otomatis ditambahkan sebagai peserta pada setiap rapat yang dibuat oleh ketua divisi mana pun (tidak perlu diundang manual), termasuk otomatis tersinkron ke Google Calendar milik Direktur.
* Ketua divisi bisa menambahkan link google doc dan menulis notulensi hasil rapat ke dalam detail rapat.
* Karyawan bisa melihat daftar rapat yang dijadwalkan di divisinya sendiri. Direktur dan Admin bisa melihat daftar rapat di semua divisi.
* Karyawan bisa melihat detail hasil rapat (dari rapat yang bisa mereka akses) yang berisi link google doc dan notulensi rapat.
* Admin atau ketua divisi bisa menunjuk karyawan tertentu sebagai notulis untuk sebuah rapat, memberi karyawan tersebut hak menulis notulensi dan menambahkan link google doc hasil rapat pada rapat itu (izin per-rapat, bukan role global).

## Tech Stack
* springboot mvc
* thymeleaf
* springboot modulith
* deployment menggunakan docker compose
* migrasi menggunakan liquibase
* database mariadb
* JPA
* RBAC (role Admin/Direktur/Ketua Divisi/Karyawan) + izin granular per-rapat untuk penunjukan notulis
* Spring Security + OAuth2 Client (login akun google)
* Google Calendar API (sync event & invite)

## Database
Kredensial disimpan di `.env` (di-gitignore), lihat `.env.example` untuk daftar variabel yang dibutuhkan (DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, GOOGLE_OAUTH_CLIENT_ID, GOOGLE_OAUTH_CLIENT_SECRET).

