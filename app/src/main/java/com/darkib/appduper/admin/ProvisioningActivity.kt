package com.darkib.appduper.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Handles the two system entry points Android 12+ requires from a profile
 * owner during provisioning:
 *
 *  - GET_PROVISIONING_MODE: tell the system we want a managed (work) profile.
 *  - ADMIN_POLICY_COMPLIANCE: final hook inside the new profile; we finish
 *    setting up the Dupe Space here.
 */
class ProvisioningActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            DevicePolicyManager.ACTION_GET_PROVISIONING_MODE -> {
                val result = Intent()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    result.putExtra(
                        DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
                    )
                }
                setResult(RESULT_OK, result)
            }
            DevicePolicyManager.ACTION_ADMIN_POLICY_COMPLIANCE -> {
                DuperAdmin.finalizeDupeSpace(this)
                setResult(RESULT_OK)
            }
            else -> setResult(RESULT_CANCELED)
        }
        finish()
    }
}
