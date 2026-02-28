package com.dailywell.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Entry point required by Health Connect permission rationale flow.
 * Opens the public privacy policy URL and returns to the app.
 */
class HealthConnectPrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URI).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
            )
        }

        finish()
    }

    companion object {
        private val PRIVACY_POLICY_URI: Uri = Uri.parse("https://dailywell.app/privacy")
    }
}
