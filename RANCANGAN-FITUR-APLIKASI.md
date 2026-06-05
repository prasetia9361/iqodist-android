# Rancangan Fitur Aplikasi Distributor IQOS & POS Penjualan

> **Dokumen Perancangan Sistem (Functional & Technical Design)**
> **Aplikasi Operasional Internal Perusahaan** untuk Manajemen Distribusi & Point of Sale Produk Heated Tobacco (IQOS)

| Atribut | Keterangan |
|---|---|
| **Nama Aplikasi (internal)** | IQODIST — *Internal Distribution & POS System* |
| **Jenis** | **Aplikasi internal perusahaan** (BUKAN produk SaaS yang dijual ke pihak lain) |
| **Versi Dokumen** | 1.1 (Draft) |
| **Tanggal** | 31 Mei 2026 |
| **Platform Target** | Aplikasi Mobile **Native** (Android/Kotlin & iOS/Swift) + Web Admin + Backend Cloud (**Rust**) |
| **Status** | Untuk Review |
| **Pemilik Dokumen** | Tim Operasional & IT Perusahaan |

> ℹ️ **Posisi Aplikasi:** IQODIST adalah **sistem operasional milik perusahaan sendiri** untuk menjalankan bisnis distribusi (B2B) dan ritel (POS). Aplikasi ini tidak dijual sebagai produk. Namun, platform dirancang **multi-entitas** sehingga satu sistem dapat melayani **kantor pusat (HQ), cabang/gudang, dan sub-distributor/mitra kecil ("tenant kecil")** dalam jaringan perusahaan — dengan isolasi data per entitas dan konsolidasi penuh di tingkat HQ.

---

## Daftar Isi

1. [Ringkasan Eksekutif](#1-ringkasan-eksekutif)
2. [Latar Belakang & Tujuan](#2-latar-belakang--tujuan)
3. [Ruang Lingkup & Model Multi-Entitas](#3-ruang-lingkup--model-multi-entitas)
4. [Persona Pengguna & Peran (RBAC)](#4-persona-pengguna--peran-rbac)
5. [Arsitektur Modul (High-Level)](#5-arsitektur-modul-high-level)
6. [Fitur Inti per Modul](#6-fitur-inti-per-modul)
7. [Fitur Advance](#7-fitur-advance)
8. [Fitur Khusus IQOS & Kepatuhan (Compliance)](#8-fitur-khusus-iqos--kepatuhan-compliance)
9. [Persyaratan Non-Fungsional](#9-persyaratan-non-fungsional)
10. [Arsitektur Teknis & Rekomendasi Tech Stack](#10-arsitektur-teknis--rekomendasi-tech-stack)
11. [Strategi Online-First & Draft Offline](#11-strategi-online-first--draft-offline)
12. [Integrasi Pihak Ketiga](#12-integrasi-pihak-ketiga)
13. [Roadmap & Fase Implementasi](#13-roadmap--fase-implementasi)
14. [KPI & Metrik Keberhasilan](#14-kpi--metrik-keberhasilan)
15. [Risiko & Mitigasi](#15-risiko--mitigasi)
16. [Glosarium](#16-glosarium)

---

## 1. Ringkasan Eksekutif

IQODIST adalah **sistem operasional internal** yang menjalankan dua pilar bisnis perusahaan:

1. **Distribusi (DMS) — B2B** — mengelola alur barang dari **pemasok/principal** (mis. afiliasi PMI) → **perusahaan (distributor)** → **retailer & sub-distributor/mitra**, termasuk *Sales Force Automation* (SFA) dan *van sales*.
2. **Point of Sale (POS) — Ritel** — transaksi penjualan langsung ke konsumen di **outlet milik perusahaan**, dengan kasir, pembayaran multi-metode, dan stok real-time.

Karena produk yang dijual adalah **heated tobacco (IQOS ILUMA, TEREA, SENTIA, dan aksesori)**, aplikasi wajib menyertakan lapisan **kepatuhan regulasi tembakau Indonesia** (verifikasi usia, manajemen cukai/pita cukai, *track & trace*, registrasi perangkat & garansi, serta verifikasi keaslian/anti-pemalsuan).

**Posisi & sifat aplikasi:**
- **Aset internal, bukan produk** — IQODIST dipakai sendiri oleh perusahaan; tidak dijual sebagai lisensi/langganan ke perusahaan lain.
- **Multi-entitas** — satu platform melayani HQ, cabang, dan **sub-distributor/mitra kecil ("tenant kecil")** dengan isolasi data + konsolidasi terpusat (lihat §3.3).

**Keunggulan sistem (key differentiators):**
- Arsitektur **online-first** — transaksi resmi (POS & penjualan) wajib daring agar **stok & uang selalu akurat** (server sebagai sumber kebenaran tunggal, bebas *oversell*); tim lapangan tetap dapat menyimpan **draft pesanan** saat offline lalu mengirimnya ketika online.
- **Compliance-by-design** — kontrol usia, cukai, dan keaslian produk tertanam di alur transaksi, bukan tempelan.
- **Lapisan AI/analitik** — peramalan permintaan, *auto-replenishment*, optimasi rute, dan deteksi anomali/fraud.

---

## 2. Latar Belakang & Tujuan

### 2.1 Permasalahan yang Diselesaikan
- Pencatatan stok & penjualan masih manual (Excel/kertas) → rawan selisih, lambat, sulit diaudit.
- Tidak ada visibilitas **secondary sales** (penjualan perusahaan → retailer/sub-distributor) dan **sell-through** (penjualan ke konsumen akhir).
- Sulit memantau aktivitas salesman di lapangan (kunjungan, order, penagihan).
- Proses kasir lambat, tidak terhubung dengan stok dan keuangan.
- Operasi multi-cabang & sub-distributor sulit dikonsolidasi tanpa sistem terpusat.
- Risiko kepatuhan: penjualan ke bawah umur, produk tanpa cukai/palsu, pelanggaran regulasi pemasaran tembakau.

### 2.2 Tujuan Sistem
| No | Tujuan | Indikator |
|---|---|---|
| T1 | Digitalisasi end-to-end alur distribusi & penjualan | 100% transaksi tercatat digital |
| T2 | Visibilitas real-time stok & penjualan multi-level | Dashboard primary/secondary/sell-through |
| T3 | Efisiensi tim lapangan | ↑ kunjungan efektif, ↓ waktu administrasi |
| T4 | Kepatuhan regulasi tembakau | 0 pelanggaran usia/cukai pada audit |
| T5 | Keputusan berbasis data | Peramalan & rekomendasi otomatis |
| T6 | Konsolidasi multi-entitas (HQ, cabang, tenant kecil) | Laporan grup terpusat & real-time |

---

## 3. Ruang Lingkup & Model Multi-Entitas

### 3.1 Termasuk (In-Scope)
- Aplikasi mobile untuk: salesman lapangan, kasir POS, staf gudang, manajer cabang, manajemen HQ.
- Backend cloud + Web Admin/Dashboard untuk manajemen & pelaporan terkonsolidasi.
- Modul: Master Data, Distribusi (DMS), SFA/Van Sales, POS, Inventory, Keuangan, CRM/Loyalty, Pelaporan.
- **Dukungan multi-entitas**: HQ, cabang/gudang, dan sub-distributor/mitra ("tenant kecil").
- Integrasi pembayaran, perpajakan (e-Faktur), dan notifikasi (WhatsApp/Push).

### 3.2 Tidak Termasuk (Out-of-Scope) — Fase Awal
- **Menjual aplikasi sebagai produk SaaS ke perusahaan lain** — IQODIST adalah sistem internal.
- Marketplace konsumen (B2C e-commerce penuh).
- Modul HRIS/payroll lengkap (hanya absensi sales).
- Akuntansi penuh (general ledger) — gunakan integrasi ke software akuntansi.

### 3.3 Model Multi-Entitas & "Tenant Kecil"
Perusahaan beroperasi sebagai satu grup dengan beberapa jenis entitas dalam **satu platform**:

| Entitas | Peran dalam Jaringan | Akses Aplikasi |
|---|---|---|
| **Kantor Pusat (HQ)** | Pemilik sistem, kebijakan, master data, konsolidasi | Penuh + lintas-entitas |
| **Cabang / Gudang Regional** | Operasi distribusi & ritel di wilayahnya | Penuh, terbatas pada entitasnya |
| **Sub-Distributor / Mitra ("Tenant Kecil")** | Memperluas jangkauan; kelola stok & penjualan sendiri di bawah jaringan | **Ringkas & ter-isolasi** |

**Prinsip desain:**
- **Satu platform, banyak entitas** — setiap entitas memiliki data ter-isolasi (stok, transaksi, pengguna), namun **HQ memperoleh konsolidasi penuh**.
- **Tenant kecil** mendapat versi ringkas: kelola stok sendiri, order ke perusahaan, jual ke retailer/konsumen — dengan **harga & kebijakan dikontrol HQ**.
- **Ekspansi hemat modal (asset-light)** — menambah jangkauan lewat sub-distributor tanpa harus membangun cabang penuh, sambil tetap menjaga kontrol & kepatuhan terpusat.
- **Bukan SaaS publik** — "tenant" di sini adalah **entitas/mitra internal dalam jaringan perusahaan**, bukan pelanggan yang membeli lisensi software.

---

## 4. Persona Pengguna & Peran (RBAC)

Aplikasi menggunakan **Role-Based Access Control (RBAC)** dengan hak akses granular dan **cakupan per entitas** (HQ / cabang / tenant kecil).

| Peran | Persona | Kebutuhan Utama | Platform |
|---|---|---|---|
| **Manajemen / Direksi (HQ)** | Pemilik & manajemen pusat | Visibilitas seluruh entitas, konsolidasi, kebijakan, master data, harga | Web + Mobile |
| **Admin Cabang / Entitas** | Kepala cabang/gudang regional | Kelola operasi entitasnya (stok, harga lokal, user, laporan) | Web + Mobile |
| **Supervisor / Manajer Sales** | Koordinator lapangan | Atur rute, target, monitor tim, approval | Mobile + Web |
| **Salesman / Kanvaser** | Tim lapangan | Kunjungan, ambil order, van sales, penagihan | Mobile |
| **Kasir** | Operator outlet ritel | Transaksi POS cepat, shift, pembayaran | Mobile/Tablet |
| **Staf Gudang** | Petugas gudang | Terima barang, stok opname, kirim barang | Mobile |
| **Staf Keuangan** | Finance | Piutang, penagihan, rekonsiliasi | Web + Mobile |
| **Sub-Distributor / Mitra (Tenant Kecil)** | Mitra usaha dalam jaringan | Akses ter-isolasi: stok sendiri, order ke perusahaan, POS/penjualan sendiri, laporan terbatas | Mobile (+Web ringkas) |
| **Retailer / Outlet Pelanggan** *(opsional)* | Toko pembeli (customer B2B) | Self-order ke perusahaan, lihat tagihan & promo | Mobile/portal |
| **Konsumen** *(opsional)* | End-user | Registrasi device, garansi, verifikasi keaslian, loyalty | Mobile |

> **Catatan:** **Pemasok/Principal** (sumber barang, mis. afiliasi PMI) adalah pihak **eksternal**, bukan pengguna aplikasi. Interaksinya melalui dokumen pembelian (PO) & penerimaan barang.

---

## 5. Arsitektur Modul (High-Level)

```
┌──────────────────────────────────────────────────────────────────┐
│      APLIKASI MOBILE NATIVE — Android (Kotlin) · iOS (Swift)        │
│  Sales App  │  POS App  │  Warehouse App  │  Manager Dashboard      │
│        (HQ · Cabang · Sub-Distributor / "Tenant Kecil")            │
└───────────────────────────────┬──────────────────────────────────┘
                                 │  (Online-first · API · Draft offline)
┌───────────────────────────────┴──────────────────────────────────┐
│          BACKEND / API LAYER — Rust (Axum) · Multi-Entitas         │
├────────────────────────────────────────────────────────────────────┤
│  A. Master Data    │  B. Distribusi (DMS)  │  C. SFA / Van Sales    │
│  D. POS            │  E. Inventory/Gudang  │  F. Keuangan & Bayar    │
│  G. CRM & Loyalty  │  H. Pelaporan/Analitik│  I. Compliance/Cukai    │
│  J. AI/ML Engine   │  K. Notifikasi        │  L. Audit & Keamanan    │
└───────────────────────────────┬──────────────────────────────────┘
                                 │
        ┌────────────────────────┴────────────────────────┐
        │  Integrasi: Payment GW · e-Faktur · WA · Maps ·   │
        │  ERP/Akuntansi · BI · Track & Trace Pemerintah    │
        └───────────────────────────────────────────────────┘
```

> Setiap entitas (HQ/cabang/tenant kecil) beroperasi pada platform yang sama dengan **isolasi data**; HQ memperoleh **konsolidasi lintas-entitas**.

---

## 6. Fitur Inti per Modul

### A. Modul Master Data
Fondasi seluruh sistem.

- **Manajemen Produk/SKU**: kode, nama, kategori (device/consumable/aksesori), varian (rasa/warna), foto, barcode/QR, satuan (pcs/slop/karton) & konversi UoM.
- **Harga Bertingkat (Tiered Pricing)**: harga per channel/segmen (grosir, retail, sub-distributor, konsumen), price list per wilayah/entitas, harga khusus per pelanggan.
- **Manajemen Outlet/Pelanggan**: profil lengkap, *geotag* lokasi, foto toko, segmentasi (A/B/C), tipe outlet, limit kredit & termin pembayaran.
- **Manajemen Entitas & Wilayah**: hierarki HQ → cabang → sub-distributor (tenant kecil); area → rute/*beat* → outlet.
- **Pajak & Cukai**: konfigurasi PPN, tarif cukai (HJE), pita cukai.
- **User & Role (RBAC)**: pembuatan akun, peran, hak akses granular **per entitas**, audit.

### B. Modul Distribusi (DMS) — B2B
- **Pembelian dari Pemasok/Principal**: *Purchase Order* (PO), penerimaan barang (*Goods Receipt Note*/GRN), pencocokan 3-arah (PO–GRN–Invoice).
- **Sales Order (SO) ke Retailer/Sub-Distributor**: pembuatan order, cek limit kredit, alokasi stok, persetujuan (*approval workflow*).
- **Tracking Penjualan Multi-Level**: barang masuk dari pemasok (*primary*), penjualan perusahaan → retailer/sub-distributor (*secondary*), plus estimasi *sell-through* ke konsumen.
- **Manajemen Kredit & Piutang**: limit kredit, *aging* piutang, blokir otomatis bila over-limit/jatuh tempo.
- **Pengiriman & Logistik**: surat jalan, *proof of delivery* (tanda tangan/foto), pelacakan status pengiriman.
- **Retur & Klaim**: retur barang rusak/expired, retur kemasan, proses klaim ke pemasok/principal.
- **Distribusi & Alokasi Stok**: alokasi stok ke cabang/sub-distributor/sales/van.

### C. Modul Sales Force Automation (SFA) & Van Sales
Untuk tim lapangan — inti efisiensi distribusi.

- **Route/Beat Planning**: rencana kunjungan harian/mingguan, optimasi rute, *journey plan adherence* (kepatuhan kunjungan).
- **Check-in/Check-out Outlet**: validasi GPS + *geofencing*, foto, durasi kunjungan.
- **Taking Order (Pre-Sales/Canvassing)**: katalog produk bergambar, order cepat, *suggested order*, promo otomatis. Dapat disimpan sebagai **draft** saat offline, lalu dikirim menjadi pesanan resmi ketika online.
- **Van Sales (Direct Store Delivery/DSD)**: stok di kendaraan, faktur & serah terima di tempat, rekonsiliasi stok van harian (load/unload). Di area tanpa sinyal: buat **draft**, lalu konfirmasi (potong stok & terbitkan faktur) saat online.
- **Penagihan (Collection)**: penagihan piutang di lapangan, cetak kuitansi, setoran.
- **Market Intelligence/Survei**: harga kompetitor, ketersediaan produk, kondisi display, survei kustom.
- **Foto Display & Planogram**: dokumentasi etalase (bisa di-upgrade ke *image recognition* — lihat Fitur Advance).
- **Absensi & GPS Tracking**: absensi berbasis lokasi, *live tracking*, jejak perjalanan.
- **Target & KPI Sales**: target penjualan/kunjungan, pencapaian real-time, leaderboard.

### D. Modul POS Penjualan (Ritel)
- **Antarmuka Kasir Cepat**: pemindaian barcode, *quick keys* produk favorit, pencarian cepat.
- **Multi-Metode Pembayaran**: tunai, kartu debit/kredit, **QRIS**, e-wallet (GoPay/OVO/DANA/ShopeePay), transfer, kredit/tempo.
- **Pembayaran Fleksibel**: *split payment*, pembayaran sebagian, uang kembalian, *down payment*.
- **Diskon & Promo**: diskon item/transaksi, voucher, *bundling*, *buy-x-get-y*, promo otomatis berbasis aturan.
- **Struk**: cetak thermal (Bluetooth/USB) + struk digital (WhatsApp/email).
- **Manajemen Shift & Kas**: buka/tutup shift, rekonsiliasi *cash drawer*, setoran kasir.
- **Hold/Resume & Retur/Refund**: simpan transaksi tertunda, retur dengan otorisasi.
- **Online-First (validasi real-time)**: transaksi POS resmi memerlukan koneksi agar stok & pembayaran tervalidasi seketika; bila offline, aplikasi **menahan/menolak** transaksi hingga online — tidak ada transaksi tersimpan diam-diam (lihat §11).
- **Multi-Outlet/Entitas**: satu sistem POS untuk seluruh outlet milik perusahaan maupun outlet sub-distributor.

### E. Modul Inventory & Gudang
- **Stok Real-time Multi-Lokasi**: per gudang/outlet/van/entitas, kartu stok.
- **Pemindaian Barcode/QR**: terima, kirim, hitung.
- **Batch, Serial Number & Kadaluarsa**: pelacakan *batch/lot*, FEFO (*First-Expired-First-Out*) — penting untuk cukai & *track & trace*.
- **Stock Opname / Cycle Count**: hitung fisik vs sistem, penyesuaian dengan *approval*.
- **Reorder Otomatis**: *min/max stock*, *reorder point*, saran pembelian.
- **Transfer Stok**: antar gudang/outlet/van/entitas dengan dokumen & konfirmasi.
- **Penyesuaian Stok**: dengan alasan & jejak audit.

### F. Modul Keuangan & Pembayaran
- **Faktur & Penagihan**: faktur otomatis dari SO/POS, faktur berulang.
- **Piutang (AR) & Utang (AP)**: *aging*, jadwal jatuh tempo, pengingat otomatis.
- **Rekonsiliasi Pembayaran**: cocokkan pembayaran dengan faktur, *payment gateway settlement*.
- **Manajemen Kas & Bank**: arus kas masuk/keluar, multi-rekening.
- **Perpajakan**: PPN, **integrasi e-Faktur (DJP)**, faktur pajak.
- **Laporan Keuangan Ringkas**: penjualan, margin, piutang, arus kas — **per entitas & terkonsolidasi** (akuntansi penuh via integrasi).

### G. Modul CRM & Loyalty
- **Database Pelanggan 360°**: B2B (outlet/sub-distributor) & B2C (konsumen), riwayat transaksi.
- **Segmentasi & Profiling**: RFM (*Recency-Frequency-Monetary*), tipe, preferensi.
- **Program Loyalty**: poin, *reward*, *tier* (mis. silver/gold), redeem.
- **Registrasi Perangkat & Garansi**: registrasi device IQOS, masa garansi, klaim, *replacement*.
- **Kampanye & Promo**: promo tertarget, kupon, program *trade* (untuk retailer/sub-distributor).
- **Komunikasi**: notifikasi push, **WhatsApp Business API**, SMS/email.
- **Customer Support/Ticketing**: keluhan, status, SLA.

### H. Modul Pelaporan & Analitik
- **Dashboard per Peran & Entitas**: tampilan relevan untuk HQ/manajer cabang/sales/kasir/sub-distributor.
- **Laporan Penjualan**: per produk/outlet/sales/wilayah/entitas/periode; primary vs secondary vs sell-through.
- **Konsolidasi Grup**: gabungan seluruh entitas untuk HQ; *drill-down* hingga per cabang/tenant.
- **Laporan Stok**: nilai stok, pergerakan, *slow/fast moving*, stok mati.
- **Laporan Keuangan**: omzet, margin, piutang, *aging*.
- **Performa Sales**: target vs realisasi, *coverage*, *productivity*, *strike rate*.
- **Ekspor & Jadwal**: PDF/Excel, laporan terjadwal via email.

---

## 7. Fitur Advance

Fitur diferensiasi yang meningkatkan nilai operasional secara signifikan.

| # | Fitur | Deskripsi & Manfaat |
|---|---|---|
| AD-1 | **Demand Forecasting (AI)** | Peramalan permintaan per SKU/outlet/wilayah berbasis histori, musiman, & tren → kurangi *stockout* & *overstock*. |
| AD-2 | **Auto-Replenishment / Smart Reorder** | Saran/otomatisasi pemesanan ulang berbasis prediksi & *lead time*. |
| AD-3 | **Route Optimization (AI)** | Optimasi rute kunjungan multi-stop (jarak, waktu, prioritas) → hemat BBM & waktu. |
| AD-4 | **Suggested Order / Recommendation Engine** | Rekomendasi produk per outlet (*cross-sell/up-sell*) berdasar pola beli serupa. |
| AD-5 | **Image Recognition (Shelf/Planogram Audit)** | Foto rak dianalisis AI untuk hitung *share of shelf*, deteksi *out-of-stock*, kepatuhan planogram. |
| AD-6 | **Dynamic Pricing & Promo Engine** | Aturan harga/promo fleksibel berbasis segmen, stok, & waktu. |
| AD-7 | **Fraud & Anomaly Detection** | Deteksi transaksi mencurigakan, manipulasi GPS (*fake GPS*), pola retur abnormal, kebocoran stok. |
| AD-8 | **Credit Scoring Retailer/Sub-Distributor** | Skor kredit otomatis berbasis histori pembayaran → tentukan limit & termin. |
| AD-9 | **Churn Prediction** | Prediksi outlet/konsumen berisiko berhenti → trigger retensi. |
| AD-10 | **Geospatial Analytics** | *Heatmap* penjualan, analisis *white-space* (area belum tergarap), kepadatan outlet. |
| AD-11 | **Gamifikasi Sales** | *Badge*, *leaderboard*, *challenge* → motivasi tim lapangan. |
| AD-12 | **AI Assistant / Chatbot** | Asisten tanya-jawab data ("omzet bulan ini?"), bantuan kasir, *natural language query*. |
| AD-13 | **Scan-to-Order / Voice Order** | Pemesanan cepat via pemindaian atau suara. |
| AD-14 | **BI Dashboard Lanjutan** | *Drill-down* lintas-entitas, *custom report builder*, *what-if analysis*. |
| AD-15 | **Digital Signature & e-Document** | Tanda tangan digital pada PO/surat jalan/kuitansi. |

---

## 8. Fitur Khusus IQOS & Kepatuhan (Compliance)

> ⚠️ **Penting:** Produk tembakau diatur ketat. Fitur berikut **wajib** untuk legalitas & mitigasi risiko. Sesuaikan dengan regulasi terkini Indonesia — al. **PP No. 28/2024** (kesehatan), **PP No. 109/2012**, **UU Cukai**, **UU PDP No. 27/2022**. *Konsultasikan dengan penasihat hukum sebelum produksi.*

| # | Fitur | Penjelasan |
|---|---|---|
| C-1 | **Verifikasi Usia (Age Verification)** | Wajib di POS & registrasi konsumen: input/scan KTP, validasi usia minimum (≥21/sesuai aturan), blokir transaksi bila tidak memenuhi. Simpan log kepatuhan. |
| C-2 | **Manajemen Cukai & Pita Cukai** | Catat & validasi pita cukai per batch, rekonsiliasi cukai, pelaporan ke Bea Cukai. |
| C-3 | **Track & Trace / Serialisasi** | Pelacakan unit/karton via *unique identifier* (QR/serial) dari gudang → outlet/sub-distributor → konsumen; dukung aggregasi (unit→slop→karton). |
| C-4 | **Verifikasi Keaslian (Anti-Counterfeit)** | Pemindaian QR keaslian produk untuk pastikan bukan barang palsu/selundupan. |
| C-5 | **Registrasi Device & Garansi** | Registrasi perangkat IQOS ILUMA (serial), aktivasi garansi, riwayat klaim & *replacement*. |
| C-6 | **Kepatuhan Pemasaran** | Pembatasan tampilan promosi sesuai regulasi (mis. tidak menarget bawah umur, *health warning* wajib). |
| C-7 | **Batas Pembelian (Anti-Bulk)** | Batasi kuantitas pembelian konsumen untuk cegah penyalahgunaan/penjualan ulang ilegal. |
| C-8 | **Health Warning & Disclaimer** | Tampilkan peringatan kesehatan wajib pada antarmuka & struk sesuai ketentuan. |
| C-9 | **Audit Trail Kepatuhan** | Catatan tak-terhapus (*immutable log*) untuk verifikasi usia, cukai, dan keaslian — siap audit, lintas seluruh entitas. |

---

## 9. Persyaratan Non-Fungsional

| Kategori | Persyaratan |
|---|---|
| **Online-First** | Transaksi resmi (POS & penjualan) wajib daring → stok/uang akurat & bebas *oversell*. Tim lapangan dapat menyimpan **draft pesanan** offline, dikirim saat online. |
| **Multi-Entitas** | Satu platform melayani HQ, cabang, & sub-distributor ("tenant kecil") dengan **isolasi data per entitas** + **konsolidasi terpusat** di HQ. |
| **Performa** | Buka transaksi < 2 dtk; pemindaian barcode instan; sinkron *delta* efisien. |
| **Keamanan** | Enkripsi data *at-rest* & *in-transit* (TLS 1.3, AES-256), RBAC per entitas, *audit log*, deteksi *fake GPS*/*root/jailbreak*. |
| **Privasi Data** | Patuh **UU PDP No. 27/2022** — *consent*, minimalisasi data, hak subjek data. |
| **Skalabilitas** | Arsitektur *cloud-native*; tumbuh dari beberapa outlet ke banyak cabang & sub-distributor tanpa re-arsitektur. |
| **Ketersediaan** | SLA ≥ 99.5%, *backup* harian, *disaster recovery*. |
| **Multi-Platform** | Aplikasi **native** — Android (Kotlin) prioritas & iOS (Swift); tablet untuk POS. |
| **Lokalisasi** | Bahasa Indonesia (utama) + Inggris; mata uang IDR; format pajak lokal. |
| **Aksesibilitas & UX** | UI sederhana untuk pengguna lapangan, *low-bandwidth friendly*, hemat baterai. |

---

## 10. Arsitektur Teknis & Rekomendasi Tech Stack

> Rekomendasi; final tergantung keahlian tim & anggaran. Arsitektur inti: **aplikasi mobile native (Kotlin/Swift)** + **backend Rust (Axum)**.

| Lapisan | Rekomendasi | Alternatif |
|---|---|---|
| **Mobile — Android** | **Kotlin + Jetpack Compose** (native) | — |
| **Mobile — iOS** | **Swift + SwiftUI** (native) | — |
| **Penyimpanan Lokal (cache & draft)** | SQLite ringan — Room/SQLDelight (Android), GRDB (iOS): simpan katalog/harga & **draft pesanan** (bukan replika seluruh DB) | Penyimpanan key-value |
| **Shared Core (opsional)** | **Rust core via UniFFI** — berbagi logika validasi & format data antar Android/iOS, selaras dengan backend | Kotlin Multiplatform |
| **Sinkronisasi** | **Tanpa engine khusus** — cukup API standar (REST/gRPC) untuk kirim transaksi & draft. Lebih hemat & sederhana | PowerSync/ElectricSQL — *hanya bila kelak butuh offline-first penuh* |
| **Backend / API** | **Rust** — **Axum** di atas Tokio (*async*, performa tinggi, hemat memori) | Actix Web |
| **ORM / Query DB** | **SQLx** (query ter-cek saat kompilasi) atau SeaORM | Diesel |
| **Background Jobs / Worker** | **apalis** (*job queue* Rust berbasis Redis) | Tokio tasks + cron |
| **Database** | PostgreSQL (transaksional, mendukung isolasi multi-entitas via schema/row-level) | MySQL |
| **Cache & Message Queue** | Redis + RabbitMQ / NATS | SQS |
| **Cloud & Deploy** | AWS / GCP (*container*, *auto-scaling*); binari Rust ringan via Docker | Azure, on-prem |
| **Autentikasi** | JWT (*crate* `jsonwebtoken`) + refresh token via middleware Axum; MFA admin | Keycloak / OIDC |
| **AI/ML** | Layanan terpisah **Python** (Prophet/scikit-learn untuk forecast; *image recognition*), dipanggil backend Rust via API | Vertex AI / SageMaker |
| **Analytics/BI** | Metabase (open-source) | Power BI, Looker |
| **Payment Gateway (ID)** | **Midtrans / Xendit / DOKU** (QRIS, VA, e-wallet, kartu) | iPaymu |
| **Notifikasi** | Firebase Cloud Messaging (push) + **WhatsApp Business API** | OneSignal |
| **Peta & Rute** | Google Maps Platform (Routes/Geocoding) | Mapbox |
| **Cetak Struk** | SDK printer thermal Bluetooth/ESC-POS | — |
| **Monitoring** | Sentry (error) + Grafana/Prometheus | Datadog |

**Mengapa native + Rust?**
- **Aplikasi native (Kotlin & Swift)** memberi performa & daya tahan baterai terbaik, serta akses paling andal ke perangkat keras penting di lapangan: pemindai barcode/kamera, printer thermal (Bluetooth/ESC-POS), GPS, dan penyimpanan **draft** offline.
- **Backend Rust** memberi performa tinggi, penggunaan memori rendah (hemat biaya server), dan keamanan memori yang menghilangkan satu kelas bug — cocok untuk inti transaksional yang harus andal 24/7.
- **Trade-off:** dua basis kode mobile (Android & iOS) lebih mahal daripada satu basis kode lintas-platform; dimitigasi dengan **shared core Rust (UniFFI)** untuk logika sinkronisasi & validasi yang dipakai ulang di kedua platform dan selaras dengan backend.

---

## 11. Strategi Online-First & Draft Offline

Pendekatan: **transaksi resmi wajib daring**, dengan kemudahan **draft offline** untuk tim lapangan. Lebih sederhana, lebih murah, dan **bebas dari risiko stok minus** dibanding *offline-first* penuh.

### 11.1 Prinsip
- **Server sumber kebenaran tunggal** — stok, harga, & saldo dihitung di server, bukan di HP → mustahil terjadi *oversell* atau angka saling menimpa.
- **Transaksi resmi butuh internet** — POS, penjualan, serta penerimaan/pengeluaran stok divalidasi *real-time* ke server. Tanpa koneksi, aplikasi **menahan** transaksi & memberi tahu kasir/sales (tidak tersimpan diam-diam).
- **Draft offline untuk lapangan** — sales boleh menyusun **pesanan draft** saat sinyal hilang (katalog & harga di-*cache* di HP). Draft **belum** memotong stok / bukan penjualan resmi.
- **Submit saat online** — begitu ada sinyal, draft dikirim ke server, divalidasi (cek stok & harga terkini), lalu menjadi transaksi resmi. Bila stok sudah habis, server **menolak & memberi tahu** — bukan menebak.

### 11.2 Pengaman Sederhana
- **ID unik per transaksi (UUID)** — server menolak kiriman ganda → aman bila sinyal putus saat mengirim (tidak dobel, tidak hilang).
- **Cache ringan di HP** — hanya katalog, harga, & daftar outlet untuk kelancaran; bukan replika seluruh database.
- **Tanpa engine sinkronisasi** — cukup API standar; menekan biaya lisensi & kerumitan, mempercepat pembuatan.

> **Catatan skala:** bila kelak operasi meluas ke banyak area *blank spot* dan benar-benar butuh **POS penuh tanpa internet**, arsitektur dapat ditingkatkan ke *offline-first* dengan engine sinkronisasi (mis. PowerSync). Desain ini dibuat agar peningkatan tersebut tidak membongkar sistem.

---

## 12. Integrasi Pihak Ketiga

| Sistem | Tujuan |
|---|---|
| **Payment Gateway** (Midtrans/Xendit) | QRIS, e-wallet, VA, kartu, *settlement*. |
| **e-Faktur / Coretax DJP** | Faktur pajak elektronik, pelaporan PPN. |
| **WhatsApp Business API** | Notifikasi order, tagihan, struk digital, promo. |
| **ERP / Akuntansi** (Accurate, Jurnal, SAP) | Sinkron data keuangan & inventory terkonsolidasi. |
| **Sistem Track & Trace Pemerintah** | Pelaporan cukai & serialisasi (jika diwajibkan). |
| **Google Maps Platform** | Geocoding outlet, optimasi rute. |
| **BI Tools** | Pelaporan & dashboard lanjutan. |
| **SMS Gateway / Email** | Notifikasi alternatif & OTP. |

---

## 13. Roadmap & Fase Implementasi

### Fase 0 — Discovery & Desain (4–6 minggu)
Riset detail, *user research*, wireframe/UI-UX, arsitektur teknis (termasuk model multi-entitas), validasi regulasi.

### Fase 1 — MVP (8–12 minggu)
**Fondasi + nilai tercepat (untuk operasi outlet/cabang pertama):**
- Master Data (produk, outlet, user, harga).
- POS dasar (transaksi, pembayaran tunai+QRIS, struk, shift) — **online-first** (validasi stok & pembayaran real-time).
- Inventory dasar (stok, terima/keluar, barcode).
- Verifikasi usia (C-1) & health warning (C-8) — *compliance minimum*.
- Autentikasi & RBAC.
- Dashboard ringkas.

### Fase 2 — DMS & SFA (10–14 minggu)
- Sales Order, tracking multi-level (primary/secondary), kredit & piutang.
- SFA: route/beat, check-in GPS, taking order, van sales, penagihan.
- Keuangan: faktur, AR/AP, integrasi payment gateway & e-Faktur.
- Pelaporan lanjutan & ekspor.
- Pengiriman & *proof of delivery*.

### Fase 3 — Multi-Entitas, Advance & Compliance Penuh (12–16 minggu)
- **Onboarding cabang & sub-distributor (tenant kecil)** + konsolidasi grup.
- AI: demand forecasting, auto-replenishment, route optimization, recommendation.
- CRM & Loyalty, registrasi device & garansi.
- Track & trace/serialisasi (C-3), manajemen cukai (C-2), anti-counterfeit (C-4).
- Image recognition, fraud detection, credit scoring.
- Integrasi ERP/akuntansi.

### Fase 4 — Optimasi & Skala (berkelanjutan)
- Hardening performa & keamanan, perluasan jaringan tenant kecil, BI lanjutan, gamifikasi, AI assistant, ekspansi fitur berbasis umpan balik.

```
Timeline (indikatif)
F0 ──► F1 (MVP) ──► F2 (DMS+SFA) ──► F3 (Multi-Entitas+Advance) ──► F4 (Scale)
 1.5bln   3bln          3bln                 4bln                    ongoing
```

---

## 14. KPI & Metrik Keberhasilan

| Area | Metrik |
|---|---|
| **Adopsi** | % sales/kasir aktif harian (DAU), % outlet/entitas ter-digitalisasi |
| **Operasional** | Waktu rata-rata transaksi POS, akurasi stok (selisih opname), *order fulfillment rate* |
| **Penjualan** | Pertumbuhan secondary sales, *outlet coverage*, *strike rate* kunjungan, *drop size* |
| **Multi-Entitas** | Jumlah cabang/sub-distributor aktif, kecepatan onboarding entitas baru, akurasi konsolidasi |
| **Keuangan** | *Aging* piutang turun, kolektibilitas naik, margin terpantau per entitas |
| **Kepatuhan** | 0 pelanggaran verifikasi usia/cukai pada audit, % produk terlacak |
| **Efisiensi Lapangan** | Kunjungan/hari, waktu administrasi turun, jarak tempuh (rute) turun |
| **AI** | Akurasi peramalan (MAPE), penurunan *stockout*/*overstock* |

---

## 15. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Perubahan regulasi tembakau | Tinggi | Arsitektur *compliance* modular & dapat dikonfigurasi; pantau regulasi; penasihat hukum. |
| Konektivitas buruk di lapangan | Sedang | Transaksi resmi online; **draft offline** untuk sales; outlet wajib internet andal (utama + cadangan, mis. tethering). |
| Resistensi adopsi pengguna | Sedang | UX sederhana, pelatihan, gamifikasi, *change management*. |
| Selisih stok / *overselling* | Rendah | Stok divalidasi *real-time* di server (online-first); ID unik anti-dobel; rekonsiliasi rutin. |
| Kebocoran data antar-entitas | Tinggi | Isolasi data per entitas (row/schema-level), RBAC ketat, audit log, patuh UU PDP. |
| *Fake GPS* / kecurangan sales | Sedang | Deteksi mock-location, validasi geofence + foto, anomaly detection. |
| Ketergantungan pemasok/integrasi | Sedang | Desain integrasi *loosely-coupled*, *fallback* manual, multi-pemasok bila memungkinkan. |
| Kompleksitas konsolidasi multi-entitas | Sedang | Model data entitas sejak awal; uji konsolidasi otomatis; *single source of truth* di HQ. |

---

## 16. Glosarium

| Istilah | Arti |
|---|---|
| **DMS** | *Distribution Management System* — sistem manajemen distribusi. |
| **SFA** | *Sales Force Automation* — otomatisasi aktivitas tim penjualan lapangan. |
| **Van Sales / DSD** | *Direct Store Delivery* — jual langsung dari stok di kendaraan. |
| **Primary Sales** | Barang masuk dari pemasok/principal ke perusahaan. |
| **Secondary Sales** | Penjualan perusahaan (distributor) → retailer/sub-distributor. |
| **Sell-through / Sell-out** | Penjualan retailer/sub-distributor → konsumen akhir. |
| **Multi-Entitas** | Satu platform melayani banyak entitas (HQ, cabang, sub-distributor) dengan isolasi data & konsolidasi terpusat. |
| **Tenant Kecil** | Sub-distributor/mitra dalam jaringan perusahaan dengan akses ter-isolasi & ringkas pada platform yang sama (bukan pelanggan SaaS eksternal). |
| **Beat / Route** | Rencana rute kunjungan outlet terjadwal. |
| **Strike Rate** | Rasio kunjungan yang menghasilkan order. |
| **Drop Size** | Rata-rata nilai order per kunjungan. |
| **FEFO** | *First-Expired-First-Out* — pengeluaran stok terdekat kadaluarsa dulu. |
| **Track & Trace** | Pelacakan produk lewat *unique identifier* sepanjang rantai pasok. |
| **QRIS** | *Quick Response Code Indonesian Standard* — standar pembayaran QR nasional. |
| **HJE** | Harga Jual Eceran (dasar perhitungan cukai). |
| **RBAC** | *Role-Based Access Control* — kontrol akses berbasis peran. |
| **Online-First** | Transaksi resmi wajib daring; server menjadi sumber kebenaran tunggal untuk stok & uang. |
| **Draft Offline** | Pesanan sementara yang dibuat sales saat tanpa sinyal; menjadi transaksi resmi setelah dikirim & divalidasi server. |

---

> **Catatan Hukum:** Produk tembakau yang dipanaskan tunduk pada regulasi cukai, kesehatan, dan pemasaran di Indonesia. Implementasi fitur kepatuhan (Bab 8) harus diverifikasi bersama penasihat hukum & otoritas terkait (Bea Cukai, Kemenkes) sebelum rilis produksi. Dokumen ini bersifat rancangan teknis-fungsional, bukan nasihat hukum.

*— Akhir Dokumen —*
