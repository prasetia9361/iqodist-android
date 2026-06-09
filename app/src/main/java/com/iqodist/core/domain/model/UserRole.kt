package com.iqodist.core.domain.model

enum class UserRole (
    val key: String,
    val displayName: String
){
    KASIR  ("KASIR", "Kasir"),
    SALESMAN ("SALESMAN", "Salesman"),
    GUDANG ("GUDANG", "Staf Gudang"),
    MANAGER ("MANAGER", "Manager Cabang"),
    ADMIN ("ADMIN", "Admin Cabang"),
    HQ ("HQ", "Manajemen HQ");

    companion object{
        fun fromKey(key: String): UserRole{
            return entries.find { it.key == key.uppercase() } ?: KASIR
        }
    }
}

enum class AppMenu(
    val label: String,
    val route: String,
    val description: String
){
    DASHBOARD ("Dashboard", "dashboard", "KPI dan ringkasan data"),
    POS ("Kasir", "pos", "Transaksi penjualan ritel"),
    INVENTORY ("Inventory", "inventory", "Stok dan gudang"),
    SFA ("Lapangan", "sfa", "Kunjungan outlet dan order"),
    REPORTS ("Laporan", "reports", "Laporan penjualan dan stok")
}

fun UserRole.getAllowedMenus(): List<AppMenu> = when (this) {
    UserRole.KASIR -> listOf(AppMenu.POS)
    UserRole.SALESMAN -> listOf(AppMenu.SFA)
    UserRole.GUDANG -> listOf(AppMenu.INVENTORY)
    UserRole.MANAGER -> listOf(
        AppMenu.DASHBOARD,
        AppMenu.POS,
        AppMenu.INVENTORY,
        AppMenu.SFA,
        AppMenu.REPORTS
    )
    UserRole.ADMIN,
    UserRole.HQ -> AppMenu.entries.toList()
}