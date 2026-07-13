package com.darkib.appduper.core

import android.content.Context
import android.content.pm.PackageManager

/**
 * Bookkeeping for cloned apps. A clone's package is the original's package plus
 * the ".dupe" suffix (with an optional index for a second/third copy), so the
 * set of clones can be recovered just by scanning installed packages.
 */
object Clones {

    private val CLONE_REGEX = Regex("^(.+)\\.dupe(\\d*)$")

    /** Maps each original package to the list of its installed clone packages. */
    fun cloneMap(context: Context): Map<String, List<String>> {
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledPackages(0).mapNotNull { it.packageName }.toHashSet()
        } catch (t: Throwable) {
            return emptyMap()
        }
        val map = HashMap<String, MutableList<String>>()
        for (pkg in installed) {
            val match = CLONE_REGEX.matchEntire(pkg) ?: continue
            val base = match.groupValues[1]
            if (base in installed) {
                map.getOrPut(base) { mutableListOf() }.add(pkg)
            }
        }
        return map
    }

    /** Original packages that currently have at least one clone. */
    fun basePackagesWithClones(context: Context): Set<String> = cloneMap(context).keys

    /** The first clone package for [basePackage], or null. */
    fun firstClone(context: Context, basePackage: String): String? =
        cloneMap(context)[basePackage]?.firstOrNull()

    fun launchIntentFor(context: Context, clonePackage: String) =
        context.packageManager.getLaunchIntentForPackage(clonePackage)

    fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
