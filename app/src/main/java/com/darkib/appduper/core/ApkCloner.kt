package com.darkib.appduper.core

import android.content.Context
import android.content.pm.PackageManager
import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.chunk.xml.ResXmlElement
import java.io.File

/**
 * Clones an installed app the "App Cloner" way: pull its APK(s), rename the
 * package id in the binary AndroidManifest (keeping component classes pointing
 * at the original package so they still resolve, and making authorities /
 * declared permissions unique so they don't collide with the original), then
 * re-sign. The result installs as a separate app with its own data — no work
 * profile, works on any device.
 */
object ApkCloner {

    const val SUFFIX = ".dupe"

    // Platform attribute resource ids (stable AOSP values).
    private const val ATTR_NAME = 0x01010003
    private const val ATTR_PERMISSION = 0x01010006
    private const val ATTR_AUTHORITIES = 0x01010018
    private const val ATTR_TARGET_ACTIVITY = 0x01010202

    data class Result(
        val success: Boolean,
        val apks: List<File> = emptyList(),
        val newPackage: String = "",
        val error: String? = null,
    )

    fun buildClone(context: Context, packageName: String): Result {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val minSdk = if (android.os.Build.VERSION.SDK_INT >= 24) appInfo.minSdkVersion else 21

            val sources = buildList {
                appInfo.sourceDir?.let { add(it) }
                appInfo.splitSourceDirs?.let { addAll(it) }
            }.map { File(it) }.filter { it.exists() }

            if (sources.isEmpty()) {
                return Result(false, error = "Couldn't find this app's APK on the device.")
            }

            val newPackage = uniquePackage(pm, packageName)
            val work = File(context.cacheDir, "clone").apply {
                deleteRecursively(); mkdirs()
            }

            val signed = sources.mapIndexed { index, source ->
                val module = ApkModule.loadApkFile(source)
                rewriteManifest(module, packageName, newPackage)
                module.refreshManifest()
                val unsigned = File(work, "u$index.apk")
                module.writeApk(unsigned)
                val out = File(work, "part$index.apk")
                Signer.sign(context, unsigned, out, minSdk.coerceAtLeast(21))
                unsigned.delete()
                out
            }

            Result(true, apks = signed, newPackage = newPackage)
        } catch (t: Throwable) {
            Result(false, error = t.message ?: t.javaClass.simpleName)
        }
    }

    private fun uniquePackage(pm: PackageManager, base: String): String {
        var candidate = base + SUFFIX
        var i = 2
        while (isInstalled(pm, candidate)) {
            candidate = "$base$SUFFIX$i"
            i++
        }
        return candidate
    }

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun rewriteManifest(module: ApkModule, oldPkg: String, newPkg: String) {
        val manifest: AndroidManifestBlock = module.androidManifest ?: return

        fun absolute(name: String?): String = when {
            name.isNullOrEmpty() -> name ?: ""
            name.startsWith(".") -> oldPkg + name
            !name.contains(".") -> "$oldPkg.$name"
            else -> name
        }

        fun attrOf(e: ResXmlElement, id: Int): ResXmlAttribute? = e.searchAttributeByResourceId(id)

        @Suppress("UNCHECKED_CAST")
        fun children(parent: ResXmlElement, tag: String): List<ResXmlElement> =
            (parent.listElements(tag) as List<ResXmlElement>)

        // Keep every component class pointing at the ORIGINAL package (its dex
        // classes live there), converting relative names to absolute first.
        val application = manifest.applicationElement
        if (application != null) {
            attrOf(application, ATTR_NAME)?.let { it.valueAsString = absolute(it.valueAsString) }
            for (tag in listOf("activity", "activity-alias", "service", "receiver", "provider")) {
                for (element in children(application, tag)) {
                    attrOf(element, ATTR_NAME)?.let { it.valueAsString = absolute(it.valueAsString) }
                    if (tag == "activity-alias") {
                        attrOf(element, ATTR_TARGET_ACTIVITY)?.let { it.valueAsString = absolute(it.valueAsString) }
                    }
                    if (tag == "provider") {
                        attrOf(element, ATTR_AUTHORITIES)?.let { a ->
                            a.valueAsString?.let { v -> a.valueAsString = v.replace(oldPkg, newPkg) }
                        }
                    }
                }
            }
        }

        val root = manifest.manifestElement
        if (root != null) {
            // Rename the app's own declared permissions so they don't clash
            // with the still-installed original (INSTALL_FAILED_DUPLICATE_PERMISSION).
            for (element in children(root, "permission")) {
                attrOf(element, ATTR_NAME)?.let { a ->
                    a.valueAsString?.let { v ->
                        if (v.startsWith(oldPkg)) a.valueAsString = newPkg + v.substring(oldPkg.length)
                    }
                }
            }
            for (element in children(root, "uses-permission")) {
                attrOf(element, ATTR_NAME)?.let { a ->
                    a.valueAsString?.let { v ->
                        if (v.startsWith("$oldPkg.")) a.valueAsString = newPkg + v.substring(oldPkg.length)
                    }
                }
            }
            // android:permission="<ownperm>" references on components.
            val attributes = root.recursiveAttributes()
            while (attributes.hasNext()) {
                val a = attributes.next() as? ResXmlAttribute ?: continue
                if (a.nameId == ATTR_PERMISSION) {
                    a.valueAsString?.let { v ->
                        if (v.startsWith("$oldPkg.")) a.valueAsString = newPkg + v.substring(oldPkg.length)
                    }
                }
            }
        }

        manifest.packageName = newPkg
    }
}
