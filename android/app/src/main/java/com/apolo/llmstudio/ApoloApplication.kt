package com.apolo.llmstudio

import android.app.Application
import android.util.Log
import com.clerk.api.Clerk

object ClerkRuntime {
    @Volatile
    var initialized: Boolean = false
        private set

    fun markInitialized(value: Boolean) {
        initialized = value
    }
}

class ApoloApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (!BuildConfig.CLERK_CONFIGURED) {
            Log.w("ApoloLLMStudio", "Clerk não inicializado: configure uma publishable key válida.")
            return
        }

        try {
            Clerk.initialize(this, publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY)
            ClerkRuntime.markInitialized(true)
        } catch (error: Exception) {
            ClerkRuntime.markInitialized(false)
            Log.e("ApoloLLMStudio", "Falha ao inicializar Clerk. O app continuará em modo de configuração.", error)
        }
    }
}
