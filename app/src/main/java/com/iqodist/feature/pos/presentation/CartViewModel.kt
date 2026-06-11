package com.iqodist.feature.pos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqodist.core.data.local.SessionManager
import com.iqodist.feature.pos.domain.model.*
import com.iqodist.feature.pos.domain.repository.ProductRepository
import com.iqodist.feature.pos.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val subtotal: StateFlow<Long> = _cartItems
        .map { items -> items.sumOf { it.subTotal } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val taxTotal: StateFlow<Long> = _cartItems
        .map { items -> items.sumOf { it.taxAmount } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val grandTotal: StateFlow<Long> = _cartItems
        .map { items -> items.sumOf { it.totalWithTax } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val hasAgeRestrictedItem: StateFlow<Boolean> = _cartItems
        .map { items -> items.any { it.isAgeRestricted } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val totalItemCount: StateFlow<Int> = _cartItems
        .map { items -> items.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private suspend fun getSessionData(): Pair<String, String> {
        val kasirId  = sessionManager.userId.first()
        val entityId = sessionManager.entityId.first()
        return Pair(kasirId, entityId)
    }

    fun onBarcodeScanned(barcode: String) {
        if (_scanState.value is ScanState.Loading) return

        viewModelScope.launch {
            _scanState.value = ScanState.Loading

            try {
                Timber.d("Mencari produk dengan barcode: $barcode")
                val product = productRepository.getProductByBarcode(barcode)

                if (product != null) {
                    addProduct(product)
                    _scanState.value = ScanState.Found(product.name)
                    Timber.d("Produk ditemukan: ${product.name}")
                } else {
                    _scanState.value = ScanState.NotFound(barcode)
                    Timber.w("Produk dengan barcode $barcode tidak ditemukan")
                }
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Gagal mencari produk")
                Timber.e(e, "Error saat scan barcode")
            }
        }
    }

    fun resetScanState() {
        _scanState.value = ScanState.Idle
    }

    fun addProduct(product: Product) {
        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.productId == product.id }

        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(
                CartItem(
                    productId       = product.id,
                    productName     = product.name,
                    barcode         = product.barcode,
                    price           = product.price,
                    quantity        = 1,
                    isAgeRestricted = product.isAgeRestricted
                )
            )
        }
        _cartItems.value = currentList
    }

    fun removeItem(productId: String) {
        _cartItems.value = _cartItems.value.filter { it.productId != productId }
    }

    fun updateQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) { removeItem(productId); return }
        _cartItems.value = _cartItems.value.map { item ->
            if (item.productId == productId) item.copy(quantity = newQuantity) else item
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _checkoutState.value = CheckoutState.Idle
        _scanState.value = ScanState.Idle
    }

    fun checkout(
        paymentMethod: PaymentMethod,
        amountPaid: Long,
        ageVerified: Boolean
    ) {
        if (_checkoutState.value is CheckoutState.Processing) return

        viewModelScope.launch {
            if (_cartItems.value.isEmpty()) {
                _checkoutState.value = CheckoutState.Error("Keranjang masih kosong")
                return@launch
            }
            if (hasAgeRestrictedItem.value && !ageVerified) {
                _checkoutState.value = CheckoutState.NeedAgeVerification
                return@launch
            }
            if (paymentMethod == PaymentMethod.CASH && amountPaid < grandTotal.value) {
                _checkoutState.value = CheckoutState.Error(
                    "Uang dibayar  " +
                    "kurang dari total "
                )
                return@launch
            }

            _checkoutState.value = CheckoutState.Processing

            val (kasirId, entityId) = getSessionData()
            Timber.d("Checkout oleh kasirId= di entityId=")

            val transaction = Transaction(
                item         = _cartItems.value,
                paymentMethod = paymentMethod,
                subTotal = subtotal.value,
                taxTotal      = taxTotal.value,
                grandTotal    = grandTotal.value,
                amountPaid    = amountPaid,
                change        = if (paymentMethod == PaymentMethod.CASH)
                                    amountPaid - grandTotal.value else 0L,
                kasirId       = kasirId,
                entityId      = entityId,
                ageVerified   = ageVerified
            )

            val result = transactionRepository.submitTransaction(transaction)

            _checkoutState.value = result.fold(
                onSuccess = { receiptId ->
                    Timber.d("Transaksi berhasil: ")
                    CheckoutState.Success(
                        receiptId   = receiptId,
                        change      = transaction.change,
                        transaction = transaction
                    )
                },
                onFailure = { error ->
                    Timber.e("Transaksi gagal: ")
                    CheckoutState.Error(error.message ?: "Transaksi gagal")
                }
            )
        }
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }
}

sealed class ScanState {
    data object Idle                         : ScanState()
    data object Loading                      : ScanState()
    data class  Found(val productName: String) : ScanState()
    data class  NotFound(val barcode: String)  : ScanState()
    data class  Error(val message: String)     : ScanState()
}

sealed class CheckoutState {
    data object Idle                       : CheckoutState()
    data object Processing                 : CheckoutState()
    data object NeedAgeVerification        : CheckoutState()
    data class  Success(
        val receiptId: String,
        val change: Long,
        val transaction: Transaction
    )                                      : CheckoutState()
    data class  Error(val message: String) : CheckoutState()
}
