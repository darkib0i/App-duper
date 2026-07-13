package com.darkib.appduper.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.darkib.appduper.core.Bridge

class DuperAdmin : DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        finalizeDupeSpace(context)
    }

    companion object {
        fun component(context: Context): ComponentName =
            ComponentName(context, DuperAdmin::class.java)

        /**
         * Runs inside the freshly created work profile: names it, opens the
         * cross-profile bridge so the main app can send clone commands, and
         * switches the profile on. Safe to call more than once.
         */
        fun finalizeDupeSpace(context: Context) {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            if (!dpm.isProfileOwnerApp(context.packageName)) return
            val admin = component(context)

            try {
                dpm.setProfileName(admin, "Dupe Space")
            } catch (e: Exception) {
                // cosmetic only
            }

            val filter = IntentFilter(Bridge.ACTION_BRIDGE).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addCrossProfileIntentFilter(
                admin, filter, DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED
            )

            dpm.setProfileEnabled(admin)
        }
    }
}
