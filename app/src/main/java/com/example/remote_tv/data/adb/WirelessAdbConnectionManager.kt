package com.example.remote_tv.data.adb

import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

class WirelessAdbConnectionManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private val appContext = context.applicationContext
    private val privateKeyFile = File(appContext.filesDir, "wireless_adb_private.pk8")
    private val certificateFile = File(appContext.filesDir, "wireless_adb_cert.der")

    private val privateKey: PrivateKey
    private val certificate: Certificate

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        setApi(Build.VERSION.SDK_INT)

        val loadedPrivateKey = readPrivateKey()
        val loadedCertificate = readCertificate()

        if (loadedPrivateKey != null && loadedCertificate != null) {
            privateKey = loadedPrivateKey
            certificate = loadedCertificate
        } else {
            val generated = generateIdentity()
            privateKey = generated.private
            certificate = generated.certificate
            persistIdentity(privateKey, certificate)
        }
    }

    override fun getPrivateKey(): PrivateKey = privateKey

    override fun getCertificate(): Certificate = certificate

    override fun getDeviceName(): String = "REMOTE_TV"

    private fun readPrivateKey(): PrivateKey? {
        if (!privateKeyFile.exists()) return null

        val privateBytes = privateKeyFile.readBytes()
        val spec = PKCS8EncodedKeySpec(privateBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(spec)
    }

    private fun readCertificate(): Certificate? {
        if (!certificateFile.exists()) return null

        val certBytes = certificateFile.readBytes()
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certBytes))
    }

    private fun generateIdentity(): IdentityMaterial {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        val certificate = createSelfSignedCertificate(keyPair)
        return IdentityMaterial(private = keyPair.private, certificate = certificate)
    }

    private fun createSelfSignedCertificate(keyPair: KeyPair): Certificate {
        val now = Date(System.currentTimeMillis() - 60_000)
        val expiresAt = Date(System.currentTimeMillis() + 3650L * 24 * 60 * 60 * 1000)
        val subject = X500Name("CN=Remote TV App")

        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            java.math.BigInteger.valueOf(System.currentTimeMillis()),
            now,
            expiresAt,
            subject,
            keyPair.public,
        )

        val contentSigner = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)

        val holder = certBuilder.build(contentSigner)
        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(holder)
    }

    private fun persistIdentity(privateKey: PrivateKey, certificate: Certificate) {
        privateKeyFile.writeBytes(privateKey.encoded)
        certificateFile.writeBytes(certificate.encoded)
    }

    private data class IdentityMaterial(
        val private: PrivateKey,
        val certificate: Certificate,
    )

    companion object {
        @Volatile
        private var instance: WirelessAdbConnectionManager? = null

        fun getInstance(context: Context): WirelessAdbConnectionManager {
            return instance ?: synchronized(this) {
                instance ?: WirelessAdbConnectionManager(context).also { instance = it }
            }
        }
    }
}
