package com.apolo.llmstudio

import android.app.Application
import com.clerk.api.Clerk

class ApoloApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Clerk.initialize(this, publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY)
    }
}
