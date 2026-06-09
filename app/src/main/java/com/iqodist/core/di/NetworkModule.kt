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


/**
 * NetworkModule — mendaftarkan semua objek jaringan ke Hilt.
 *
 * @Module  → menandai ini adalah Hilt Module (kumpulan "resep" membuat objek)
 * @InstallIn(SingletonComponent::class) → objek yang dibuat hidup selama app hidup
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Menyediakan konfigurasi JSON parser.
     * ignoreUnknownKeys = true → tidak error jika server kirim field yang tidak ada di DTO kita
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Menyediakan OkHttpClient — mesin HTTP yang sebenarnya.
     * Di sini kita pasang:
     * - AuthInterceptor: otomatis sisipkan token
     * - LoggingInterceptor: tampilkan log request/response di Logcat (hanya saat DEBUG)
     * - Timeout: batas waktu tunggu server
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Interceptor untuk token JWT
            .addInterceptor(authInterceptor)
            // Interceptor untuk logging — hanya aktif saat build DEBUG
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY  // tampilkan semua detail
                    } else {
                        HttpLoggingInterceptor.Level.NONE  // tidak ada log di production
                    }
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)  // batas waktu koneksi ke server
            .readTimeout(30, TimeUnit.SECONDS)     // batas waktu baca response
            .writeTimeout(30, TimeUnit.SECONDS)    // batas waktu kirim request
            .build()
    }

    /**
     * Menyediakan Retrofit — library yang mengubah interface Kotlin
     * menjadi HTTP request secara otomatis.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://bullpen-vagrancy-sanitary.ngrok-free.dev/")
            .client(okHttpClient)
            // Converter: ubah JSON dari server menjadi data class Kotlin (dan sebaliknya)
            .addConverterFactory(
                json.asConverterFactory("application/json; charset=UTF-8".toMediaType())
            )
            .build()
    }
    /**
     * Menyediakan AuthApiService.
     * Retrofit.create() membuat implementasi otomatis dari interface AuthApiService.
     */
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
}