package com.iqodist.core.data.remote.interceptor

import com.iqodist.core.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * AuthInterceptor — otomatis tambahkan token JWT ke setiap HTTP request.
 *
 * Cara kerja:
 * 1. Setiap kali Retrofit akan kirim request, interceptor ini berjalan dulu
 * 2. Ia ambil token dari SessionManager (DataStore)
 * 3. Ia tambahkan header "Authorization: Bearer <token>" ke request
 * 4. Baru request dikirim ke server
 *
 * Dengan ini, semua API call otomatis terautentikasi tanpa perlu tulis manual.
 */
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Ambil token dari DataStore (runBlocking karena intercept bukan suspend function)
        val token = runBlocking {
            sessionManager.accessToken.first()
        }

        // Buat request baru dengan header Authorization ditambahkan
        val requestBuilder = chain.request().newBuilder()

        // Hanya tambahkan header jika token ada (tidak null dan tidak kosong)
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // Lanjutkan request ke server
        return chain.proceed(requestBuilder.build())
    }
}