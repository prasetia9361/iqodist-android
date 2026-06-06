package com.iqodist.navigation

// Semua route screen didefinisikan di sini sebagai sealed class
// Tambahkan argumen navigasi sebagai property jika dibutuhkan
sealed class Route(val path: String) {

    // ── Auth ──────────────────────────────────────────────────────────────
    data object Login       : Route("login")

    // ── Main (setelah login) ──────────────────────────────────────────────
    data object Dashboard   : Route("dashboard")

    // ── POS ───────────────────────────────────────────────────────────────
    data object Pos         : Route("pos")
    data object PosPayment  : Route("pos/payment")

    // ── Inventory ─────────────────────────────────────────────────────────
    data object Inventory   : Route("inventory")
    data object StockOpname : Route("inventory/opname")

    // ── SFA ───────────────────────────────────────────────────────────────
    data object Sfa         : Route("sfa")
    data object SfaCheckIn  : Route("sfa/checkin")
    data object SfaOrder    : Route("sfa/order")

    // ── Route dengan argumen — contoh detail produk ────────────────────────
    data object ProductDetail : Route("product/{productId}") {
        fun createRoute(productId: String) = "product/productId"
    }
}
