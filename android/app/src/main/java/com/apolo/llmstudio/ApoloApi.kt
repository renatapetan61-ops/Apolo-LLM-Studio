package com.apolo.llmstudio

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.successOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

object ApoloApi {
    private val http = OkHttpClient()

    suspend fun checkStatus(): String {
        val token = Clerk.auth.getToken().successOrNull()
            ?: throw IOException("Não foi possível obter o token da sessão Clerk. Entre novamente.")

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}/v1/status")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("A API respondeu HTTP ${response.code}.")
                }
                val json = JSONObject(body)
                if (!json.optBoolean("authenticated", false)) {
                    throw IOException("A API não confirmou a autenticação.")
                }
                "Conectado e autenticado. O executor Linux ainda não foi provisionado."
            }
        }
    }
}
