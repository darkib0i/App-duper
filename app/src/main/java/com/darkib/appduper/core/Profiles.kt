package com.darkib.appduper.core

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.net.Uri
import android.provider.Settings
import com.darkib.appduper.admin.DuperAdmin

/**
 * Everything related to finding, creating and talking to the Dupe Space
 * (the managed work profile that hosts the duplicated apps).
 */
object Profiles {

    /** True when this process is running inside the Dupe Space itself. */
    fun isInsideDupeSpace(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm.isProfileOwnerApp(context.packageName)
    }

    fun isManagedProfileSupported(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)

    fun isProvisioningAllowed(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
    }

    /**
     * Returns the [UserHandle] of the Dupe Space, or null if it doesn't exist yet.
     * We recognise our own profile by the fact that App Duper is installed in it.
     */
    fun dupeSpace(context: Context): UserHandle? {
        if (isInsideDupeSpace(context)) return null
        val um = context.getSystemService(UserManager::class.java)
        val la = context.getSystemService(LauncherApps::class.java)
        val me = Process.myUserHandle()
        return um.userProfiles.filter { it != me }.firstOrNull { profile ->
            try {
                la.getApplicationInfo(context.packageName, 0, profile) != null
            } catch (e: Exception) {
                false
            }
        }
    }

    /** Intent that asks Android to create the managed profile with us as owner. */
    fun provisioningIntent(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                DuperAdmin.component(context)
            )
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
        }

    /**
     * Sends a command to our twin inside the Dupe Space by explicitly routing the
     * intent through the system's cross-profile intent forwarder.
     */
    fun sendToDupeSpace(context: Context, command: String, packageName: String): Boolean {
        val intent = Intent(Bridge.ACTION_BRIDGE).apply {
            putExtra(Bridge.EXTRA_COMMAND, command)
            putExtra(Bridge.EXTRA_PACKAGE, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val candidates = context.packageManager.queryIntentActivities(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        )
        // The system forwarder that hops the profile boundary lives in package "android".
        val forwarder = candidates.firstOrNull { it.activityInfo.packageName == "android" }
            ?: return false
        intent.component = ComponentName(
            forwarder.activityInfo.packageName,
            forwarder.activityInfo.name
        )
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Package names of the apps currently duplicated into the Dupe Space. */
    fun dupedPackages(context: Context): Set<String> {
        val profile = dupeSpace(context) ?: return emptySet()
        val la = context.getSystemService(LauncherApps::class.java)
        return la.getActivityList(null, profile)
            .map { it.applicationInfo.packageName }
            .toSet() - context.packageName
    }

    /**
     * True when App Duper can provision its own Dupe Space. This is false when
     * the device already has a managed profile (Android allows only one), which
     * is exactly the case the old UI dead-ended on.
     */
    fun canCreateOwnSpace(context: Context): Boolean =
        isManagedProfileSupported(context) && isProvisioningAllowed(context)

    /** What [openSystemClone] actually managed to open, so the UI can guide accordingly. */
    enum class CloneLaunch { OEM_CLONER, APP_DETAILS, SETTINGS, NONE }

    /**
     * Fallback for devices that already have a work profile (so App Duper can't
     * own its own Dupe Space): hand off to the OS / OEM built-in cloning UI.
     *
     * Every candidate is *tried*, not just resolved — some OEM screens resolve
     * but throw on launch because they aren't exported to third-party apps, so
     * we must keep going instead of failing on the first one. It ends on the
     * plain Settings screen, which always opens, so the user is never left with
     * a dead button.
     */
    fun openSystemClone(context: Context, packageName: String): CloneLaunch {
        val oemCloners = listOf(
            // Xiaomi / MIUI / HyperOS "Dual apps"
            Intent("miui.intent.action.APP_DUAL_APPS").setPackage("com.android.settings"),
            // Samsung "Dual Messenger"
            Intent("com.samsung.android.settings.DUAL_APPS"),
            Intent().setClassName(
                "com.samsung.android.mateagent",
                "com.samsung.android.mateagent.MainActivity",
            ),
            // Oppo / Realme / OnePlus (ColorOS) "App Clone"
            Intent("com.coloros.settings.action.APP_CLONE"),
            Intent("oppo.settings.action.APP_CLONE"),
            // Generic action seen on several OEMs
            Intent("android.settings.DUAL_APPS_SETTINGS"),
        )
        for (intent in oemCloners) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null && tryStart(context, intent)) {
                return CloneLaunch.OEM_CLONER
            }
        }
        // App-specific info page: where several OEMs place the clone toggle.
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryStart(context, details)) return CloneLaunch.APP_DETAILS
        // Guaranteed: the top-level Settings screen always opens.
        val settings = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryStart(context, settings)) return CloneLaunch.SETTINGS
        return CloneLaunch.NONE
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent); true
    } catch (e: Exception) {
        false
    }

    /** Launches the duplicated copy of [packageName] living in the Dupe Space. */
    fun launchDupe(context: Context, packageName: String): Boolean {
        val profile = dupeSpace(context) ?: return false
        val la = context.getSystemService(LauncherApps::class.java)
        val activity = la.getActivityList(packageName, profile).firstOrNull() ?: return false
        return try {
            la.startMainActivity(activity.componentName, profile, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }
}
