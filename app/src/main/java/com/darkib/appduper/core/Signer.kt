package com.darkib.appduper.core

import android.content.Context
import com.android.apksig.ApkSigner
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

/** Re-signs a repackaged APK with App Duper's bundled key using apksig. */
object Signer {

    private const val KEYSTORE_ASSET = "dupe.p12"
    private const val ALIAS = "dupe"
    private val PASSWORD = "appduper".toCharArray()

    fun sign(context: Context, input: File, output: File, minSdk: Int) {
        val keyStore = KeyStore.getInstance("PKCS12")
        context.assets.open(KEYSTORE_ASSET).use { keyStore.load(it, PASSWORD) }
        val privateKey = keyStore.getKey(ALIAS, PASSWORD) as PrivateKey
        val certificate = keyStore.getCertificate(ALIAS) as X509Certificate

        val signerConfig = ApkSigner.SignerConfig.Builder(ALIAS, privateKey, listOf(certificate)).build()

        if (output.exists()) output.delete()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(input)
            .setOutputApk(output)
            .setMinSdkVersion(minSdk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setAlignmentPreserved(false)
            .build()
            .sign()
    }
}
