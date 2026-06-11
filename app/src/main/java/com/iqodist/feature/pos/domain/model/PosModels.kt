package com.iqodist.feature.pos.domain.model

import retrofit2.http.GET
import java.util.UUID
import kotlin.reflect.KProperty

data class Product(
    val id: String,
    val name: String,
    val barcode: String,
    val price: Long,
    val stockQuantity: Int,
    val isAgeRestricted: Boolean,
    val imageUrl: String? = null,
    val category: String = ""
)

data class CartItem(
    val productId: String,
    val productName: String,
    val barcode: String,
    val price: Long,
    val quantity: Int,
    val isAgeRestricted: Boolean,
    val taxRate: Double = 0.11
){
    val subTotal: Long get() = price * quantity
    val taxAmount: Long get() = ((subTotal * taxRate).toLong())
    val totalWithTax: Long get() = subTotal + taxAmount
}

enum class PaymentMethod( val displayName: String) {
    CASH ("Tunai"),
    QRIS ("QRIS"),
    DEBIT ("Kartu Debit"),
    CREDIT ("Kartu Kredit"),
    EWALLET ("Dompet Digital")
}

enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val item: List<CartItem>,
    val paymentMethod: PaymentMethod,
    val subTotal: Long,
    val discount: Long = 0,
    val taxTotal: Long,
    val grandTotal: Long,
    val amountPaid: Long,
    val change: Long = 0,
    val kasirId: String,
    val entityId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.PENDING,
    val ageVerified: Boolean = false
)

fun Long.toRupiaFormat(): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID"))
    return "Rp ${formatter.format(this)}"
    // Output: "Rp 55.000"
}