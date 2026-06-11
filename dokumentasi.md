# Dokumentasi Master Proyek IQODIST
**Sistem Distribusi & POS Internal**

Dokumen ini berisi salinan lengkap seluruh kode program utama aplikasi IQODIST, dikelompokkan berdasarkan modul dan lapisan arsitektur (Clean Architecture).

---

## 1. Konfigurasi Global & Aplikasi

### 1.1. IqodistApplication.kt
```kotlin
package com.iqodist
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class IqodistApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
```

### 1.2. MainActivity.kt
```kotlin
package com.iqodist
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.iqodist.core.data.local.SessionManager
import com.iqodist.core.ui.theme.IqodistTheme
import com.iqodist.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sessionManager: SessionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IqodistTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, sessionManager = sessionManager)
            }
        }
    }
}
```

---

## 2. Core Module (Fondasi Aplikasi)

### 2.1. Network & API Configuration
**NetworkModule.kt**
```kotlin
package com.iqodist.core.di
import com.iqodist.BuildConfig
import com.iqodist.core.data.remote.interceptor.AuthInterceptor
import com.iqodist.feature.auth.data.remote.AuthApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService = retrofit.create(AuthApiService::class.java)
}
```

**AuthInterceptor.kt**
```kotlin
package com.iqodist.core.data.remote.interceptor
import com.iqodist.core.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionManager.accessToken.first() }
        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}
```

### 2.2. Session Management (DataStore)
**SessionKeys.kt**
```kotlin
package com.iqodist.core.data.local
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SessionKeys {
    val ACCESS_TOKEN  = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val USER_ROLE     = stringPreferencesKey("user_role")
    val USER_ID       = stringPreferencesKey("user_id")
    val USER_NAME     = stringPreferencesKey("user_name")
    val ENTITY_ID     = stringPreferencesKey("entity_id")
    val IS_LOGGED_IN  = booleanPreferencesKey("is_logged_in")
}
```

**SessionManager.kt**
```kotlin
package com.iqodist.core.data.local
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iqodist_session")

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun saveSession(accessToken: String, refreshToken: String, userId: String, userName: String, userRole: String, entityId: String) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.ACCESS_TOKEN] = accessToken
            prefs[SessionKeys.REFRESH_TOKEN] = refreshToken
            prefs[SessionKeys.USER_ID] = userId
            prefs[SessionKeys.USER_NAME] = userName
            prefs[SessionKeys.USER_ROLE] = userRole
            prefs[SessionKeys.ENTITY_ID] = entityId
            prefs[SessionKeys.IS_LOGGED_IN] = true
        }
    }
    suspend fun clearSession() { context.dataStore.edit { it.clear() } }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[SessionKeys.IS_LOGGED_IN] ?: false }
    val userRole: Flow<String?> = context.dataStore.data.map { it[SessionKeys.USER_ROLE] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[SessionKeys.ACCESS_TOKEN] }
    val userName: Flow<String?> = context.dataStore.data.map { it[SessionKeys.USER_NAME] }
    val userId: Flow<String> = context.dataStore.data.map { it[SessionKeys.USER_ID] ?: "" }
    val entityId: Flow<String> = context.dataStore.data.map { it[SessionKeys.ENTITY_ID] ?: "" }
}
```

### 2.3. Theme & Styling
**Color.kt**
```kotlin
package com.iqodist.core.ui.theme
import androidx.compose.ui.graphics.Color

val IqoPrimary = Color(0xFF1D5FA8)
val IqoPrimaryLight = Color(0xFFD6E7F7)
val IqoSecondary = Color(0xFF1D9E75)
val IqoError = Color(0xFFE24B4A)
val Gray50 = Color(0xFFF1EFE8)
val Gray900 = Color(0xFF2C2C2A)
val BackgroundDark = Color(0xFF111314)
val SurfaceDark = Color(0xFF1A1C1E)
val IqoPrimaryDarkTheme = Color(0xFF92C4F0)
```

**Theme.kt**
```kotlin
package com.iqodist.core.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = IqoPrimary,
    primaryContainer = IqoPrimaryLight,
    secondary = IqoSecondary,
    background = Gray50,
    surface = androidx.compose.ui.graphics.Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = IqoPrimaryDarkTheme,
    background = BackgroundDark,
    surface = SurfaceDark
)

@Composable
fun IqodistTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = IqodistTypography, content = content)
}
```

---

## 3. Feature: POS (Point of Sale / Kasir)

### 3.1. Domain Layer (Model & Repository)
**PosModels.kt**
```kotlin
package com.iqodist.feature.pos.domain.model
import java.util.UUID

data class Product(
    val id: String, val name: String, val barcode: String,
    val price: Long, val stockQuantity: Int, val isAgeRestricted: Boolean
)

data class CartItem(
    val productId: String, val productName: String, val barcode: String,
    val price: Long, val quantity: Int, val isAgeRestricted: Boolean,
    val taxRate: Double = 0.11
) {
    val subTotal: Long get() = price * quantity
    val taxAmount: Long get() = (subTotal * 0.11).toLong()
    val totalWithTax: Long get() = subTotal + taxAmount
}

enum class PaymentMethod(val displayName: String) {
    CASH("Tunai"), QRIS("QRIS"), DEBIT("Debit")
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val item: List<CartItem>, val paymentMethod: PaymentMethod,
    val subTotal: Long, val taxTotal: Long, val grandTotal: Long,
    val amountPaid: Long, val change: Long, val kasirId: String,
    val entityId: String, val timestamp: Long = System.currentTimeMillis(),
    val ageVerified: Boolean = false
)

fun Long.toRupiaFormat(): String = "Rp ${String.format("%,d", this).replace(',', '.')}"
```

**ProductRepository.kt**
```kotlin
package com.iqodist.feature.pos.domain.repository
import com.iqodist.feature.pos.domain.model.Product

interface ProductRepository {
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun getAllProducts(): List<Product>
}
```

### 3.2. Data Layer (Implementation & DTO)
**ProductRepositoryImpl.kt**
```kotlin
package com.iqodist.feature.pos.data.repository
import com.iqodist.feature.pos.data.remote.ProductApiService
import com.iqodist.feature.pos.domain.model.Product
import com.iqodist.feature.pos.domain.repository.ProductRepository
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(private val apiService: ProductApiService) : ProductRepository {
    override suspend fun getProductByBarcode(barcode: String): Product? {
        return try {
            val dto = apiService.getProductByBarcode(barcode)
            Product(dto.id, dto.name, dto.barcode, dto.price, dto.stockQuantity, dto.isAgeRestricted)
        } catch (e: Exception) { null }
    }
    override suspend fun getAllProducts(): List<Product> = emptyList()
}
```

**PosModule.kt (Hilt DI)**
```kotlin
package com.iqodist.feature.pos.data.di
import com.iqodist.feature.pos.data.remote.*
import com.iqodist.feature.pos.data.repository.*
import com.iqodist.feature.pos.domain.repository.*
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PosModule {
    @Binds @Singleton abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
    @Binds @Singleton abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    companion object {
        @Provides @Singleton fun provideProductApiService(retrofit: Retrofit): ProductApiService = retrofit.create(ProductApiService::class.java)
        @Provides @Singleton fun provideTransactionApiService(retrofit: Retrofit): TransactionApiService = retrofit.create(TransactionApiService::class.java)
    }
}
```

### 3.3. Presentation Layer (UI & ViewModel)
**CartViewModel.kt**
```kotlin
package com.iqodist.feature.pos.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqodist.core.data.local.SessionManager
import com.iqodist.feature.pos.domain.model.*
import com.iqodist.feature.pos.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems = _cartItems.asStateFlow()
    
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState = _checkoutState.asStateFlow()

    val grandTotal = _cartItems.map { it.sumOf { item -> item.totalWithTax } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val hasAgeRestrictedItem = _cartItems.map { it.any { item -> item.isAgeRestricted } }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = productRepository.getProductByBarcode(barcode)
            product?.let { addProduct(it) }
        }
    }

    private fun addProduct(product: Product) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.productId == product.id }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = current[index].quantity + 1)
        } else {
            current.add(CartItem(product.id, product.name, product.barcode, product.price, 1, product.isAgeRestricted))
        }
        _cartItems.value = current
    }

    fun clearCart() { _cartItems.value = emptyList(); _checkoutState.value = CheckoutState.Idle }
}
```

**PosScreen.kt**
```kotlin
package com.iqodist.feature.pos.presentation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PosScreen(onNavigateUp: () -> Unit, viewModel: CartViewModel = hiltViewModel()) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val grandTotal by viewModel.grandTotal.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Kasir POS") }, navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, null) } }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { item ->
                    Text("${item.productName} x${item.quantity}")
                }
            }
            Button(onClick = { /* Bayar */ }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Bayar ${grandTotal}")
            }
        }
    }
}
```

---

## 4. Feature: Auth (Autentikasi)

### 4.1. Auth Modules
**UserRole.kt**
```kotlin
package com.iqodist.core.domain.model

enum class UserRole(val key: String, val displayName: String) {
    KASIR("KASIR", "Kasir"), SALESMAN("SALESMAN", "Salesman"), 
    GUDANG("GUDANG", "Staf Gudang"), MANAGER("MANAGER", "Manager Cabang"),
    ADMIN("ADMIN", "Admin Cabang"), HQ("HQ", "Manajemen HQ");

    companion object {
        fun fromKey(key: String): UserRole = entries.find { it.key == key.uppercase() } ?: KASIR
    }
}

enum class AppMenu(val label: String, val route: String, val description: String) {
    DASHBOARD("Dashboard", "dashboard", "KPI"), POS("Kasir", "pos", "Transaksi"),
    INVENTORY("Inventory", "inventory", "Stok"), SFA("Lapangan", "sfa", "Kunjungan")
}

fun UserRole.getAllowedMenus(): List<AppMenu> = when (this) {
    UserRole.KASIR -> listOf(AppMenu.POS)
    UserRole.MANAGER -> AppMenu.entries.toList()
    else -> listOf(AppMenu.DASHBOARD)
}
```

**LoginViewModel.kt**
```kotlin
package com.iqodist.feature.auth.presentation
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.iqodist.feature.auth.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginUseCase: LoginUseCase) : ViewModel() {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var selectedEntityId by mutableStateOf("0")
    
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun login() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = loginUseCase(username.trim(), password, selectedEntityId)
            _uiState.value = result.fold(
                onSuccess = { LoginUiState.Success(it) },
                onFailure = { LoginUiState.Error(it.message ?: "Login Gagal") }
            )
        }
    }
}
```

---

## 5. Navigation

**Route.kt**
```kotlin
package com.iqodist.navigation
sealed class Route(val path: String) {
    data object Login : Route("login")
    data object Dashboard : Route("dashboard")
    data object Pos : Route("pos")
    data object Inventory : Route("inventory")
    data object Sfa : Route("sfa")
}
```

**AppNavGraph.kt**
```kotlin
package com.iqodist.navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.iqodist.core.data.local.SessionManager
import com.iqodist.feature.auth.presentation.LoginScreen
import com.iqodist.feature.dashboard.presentation.DashboardScreen
import com.iqodist.feature.pos.presentation.PosScreen

@Composable
fun AppNavGraph(navController: NavHostController, sessionManager: SessionManager) {
    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val startDestination = if (isLoggedIn) Route.Dashboard.path else Route.Login.path

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.Login.path) { LoginScreen(onLoginSuccess = { navController.navigate(Route.Dashboard.path) { popUpTo(Route.Login.path) { inclusive = true } } }) }
        composable(Route.Dashboard.path) { DashboardScreen(onNavigate = { navController.navigate(it) }, onLogout = { navController.navigate(Route.Login.path) { popUpTo(0) { inclusive = true } } }) }
        composable(Route.Pos.path) { PosScreen(onNavigateUp = { navController.navigateUp() }) }
    }
}
```

---
*Dokumentasi ini mencakup seluruh kode program utama projek IQODIST per tanggal hari ini.*
