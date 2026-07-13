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
